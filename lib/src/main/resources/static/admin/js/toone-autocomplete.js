// Searchable combobox for to-one fields pointing at large tables (no external widget dependency).
// Each ".admin-autocomplete-input" lazy-loads options from a paginated JSON endpoint and stores the
// selected entity id in a sibling hidden input (the one actually submitted with the form).
(() => {
    const DEBOUNCE_MS = 200;
    const SCROLL_LOAD_THRESHOLD_PX = 8;
    const REQUEST_TIMEOUT_MS = 10000;

    class ToOneAutocomplete {
        // Wired DOM elements, endpoint and localized status messages, fixed for the widget's lifetime.
        #input;
        #url;
        #hidden;
        #list;
        #messages;

        #debounceTimer = null;
        #activeController = null;
        #options = [];
        #activeIndex = -1;

        // Paging state for the currently displayed result set.
        #currentTerm = '';
        #currentPage = 1;
        #hasMore = false;
        #loading = false;

        // Monotonic id: only the latest request is allowed to mutate the list, so an aborted or
        // superseded fetch can never reopen or corrupt it.
        #requestSeq = 0;

        // Last committed selection, restored when the user types but leaves without picking an
        // option — typing alone must never clear the stored relation.
        #selectedId;
        #selectedText;

        // Committed relation id whose label lookup failed; retried when the field regains focus.
        #pendingCurrentId = null;

        constructor({input, url, hidden, list}) {
            this.#input = input;
            this.#url = url;
            this.#hidden = hidden;
            this.#list = list;
            this.#selectedId = hidden.value;
            this.#selectedText = input.value;
            this.#messages = {
                noResults: input.getAttribute('data-msg-no-results') || 'No matches',
                error: input.getAttribute('data-msg-error') || 'Could not load options'
            };

            input.addEventListener('input', this.#onInput);
            input.addEventListener('keydown', this.#onKeydown);
            input.addEventListener('focus', this.#onFocus);
            input.addEventListener('blur', this.#onBlur);
            list.addEventListener('scroll', this.#onScroll);
            list.addEventListener('mousedown', this.#onListMousedown);
            // Enter-key submits skip the blur handler; make sure the same restore applies.
            input.form?.addEventListener('submit', this.#commitOrRestoreSelection);

            this.#closeList();
            this.#prefill();
        }

        #optionElementId(index) {
            return `${this.#input.id}-option-${index}`;
        }

        #cancelPendingWork() {
            clearTimeout(this.#debounceTimer);
            this.#debounceTimer = null;
            const controller = this.#activeController;
            this.#activeController = null;
            controller?.abort();
            this.#requestSeq++;
            this.#loading = false;
        }

        #closeList() {
            // Pending work must neither consume resources nor reopen the closed list.
            this.#cancelPendingWork();
            this.#hasMore = false;
            this.#list.hidden = true;
            this.#list.replaceChildren();
            this.#options = [];
            this.#activeIndex = -1;
            this.#input.setAttribute('aria-expanded', 'false');
            this.#input.removeAttribute('aria-activedescendant');
        }

        #setActiveIndex(index) {
            const children = this.#list.children;
            const previous = children[this.#activeIndex];
            if (previous) {
                previous.classList.remove('active');
                previous.removeAttribute('aria-selected');
            }
            this.#activeIndex = index;
            const next = children[this.#activeIndex];
            if (next) {
                next.classList.add('active');
                next.setAttribute('aria-selected', 'true');
                next.scrollIntoView({block: 'nearest'});
                this.#input.setAttribute('aria-activedescendant', this.#optionElementId(this.#activeIndex));
            } else {
                this.#input.removeAttribute('aria-activedescendant');
            }
        }

        #selectOption(option) {
            this.#hidden.value = option.id;
            this.#input.value = option.text;
            this.#selectedId = option.id;
            this.#selectedText = option.text;
            // Whatever id was awaiting label resolution has been replaced by an explicit pick.
            this.#pendingCurrentId = null;
            this.#closeList();
        }

        #createOptionElement(option, index) {
            const item = document.createElement('li');
            item.id = this.#optionElementId(index);
            item.className = 'admin-autocomplete-option';
            item.dataset.optionIndex = index;
            item.setAttribute('role', 'option');
            item.textContent = option.text;
            return item;
        }

        #appendOptions(results) {
            const startIndex = this.#options.length;
            const fragment = document.createDocumentFragment();
            results.forEach((option, offset) => {
                fragment.append(this.#createOptionElement(option, startIndex + offset));
            });
            this.#options.push(...results);
            this.#list.append(fragment);
        }

        #removeStatusRow() {
            this.#list.querySelector('.admin-autocomplete-status')?.remove();
        }

        // Non-selectable informational row ("no matches" / "load failed"). Not an option: never
        // active, never in aria-activedescendant. `append` keeps already-loaded options visible
        // and adds the row after them; otherwise the row replaces the list contents.
        #showStatus(message, append) {
            if (append) {
                this.#removeStatusRow();
            } else {
                this.#list.replaceChildren();
                this.#list.scrollTop = 0;
                this.#options = [];
                this.#activeIndex = -1;
                this.#input.removeAttribute('aria-activedescendant');
            }
            const status = document.createElement('li');
            status.className = 'admin-autocomplete-status';
            status.textContent = message;
            this.#list.append(status);
            this.#list.hidden = false;
            this.#input.setAttribute('aria-expanded', 'true');
        }

        #render(results, append) {
            if (!append) {
                this.#list.replaceChildren();
                this.#list.scrollTop = 0;
                this.#options = [];
            } else {
                // A leftover status row would break the option-index <-> list-child alignment.
                this.#removeStatusRow();
            }
            this.#appendOptions(results);
            this.#list.hidden = this.#options.length === 0;
            this.#input.setAttribute('aria-expanded', String(this.#options.length > 0));
            if (!append) {
                this.#setActiveIndex(this.#options.length > 0 ? 0 : -1);
            }
        }

        // Resolves to: parsed JSON on success, {error: true} on HTTP/network failure or timeout,
        // null when superseded by a newer request (no list update allowed).
        async #fetchOptions(queryString) {
            this.#activeController?.abort();
            const controller = new AbortController();
            this.#activeController = controller;
            // A hung request must not pin a connection (or the loading flag) forever. setTimeout
            // instead of AbortSignal.timeout/any keeps the widget alive on browsers lacking them.
            let timedOut = false;
            const timeoutId = setTimeout(() => {
                timedOut = true;
                controller.abort();
            }, REQUEST_TIMEOUT_MS);
            try {
                const response = await fetch(this.#url + queryString, {
                    headers: {'Accept': 'application/json'},
                    signal: controller.signal
                });
                return response.ok ? await response.json() : {error: true};
            } catch (error) {
                if (error?.name === 'AbortError' && !timedOut) {
                    // Superseded by a newer request, which owns the list now: signal "no update"
                    // rather than a failure the user should see.
                    return null;
                }
                return {error: true};
            } finally {
                clearTimeout(timeoutId);
                if (controller === this.#activeController) {
                    this.#activeController = null;
                }
            }
        }

        async #loadPage(term, page, append) {
            const seq = ++this.#requestSeq;
            this.#loading = true;
            const params = new URLSearchParams({q: term || '', page});
            const data = await this.#fetchOptions(`?${params}`);
            if (seq !== this.#requestSeq) {
                // Superseded (or aborted) by a newer request, which owns the list and loading flag now.
                return;
            }
            this.#loading = false;
            if (!data) {
                return;
            }
            if (data.error) {
                this.#hasMore = false;
                this.#showStatus(this.#messages.error, append);
                return;
            }
            this.#hasMore = Boolean(data.hasMore);
            this.#currentTerm = term;
            this.#currentPage = page;
            const results = data.results || [];
            if (results.length === 0 && !append) {
                this.#showStatus(this.#messages.noResults, false);
                return;
            }
            this.#render(results, append);
            if (results.length > 0) {
                // Not chaining after an empty page guards against a server that keeps claiming
                // hasMore without delivering rows.
                this.#fillVisibleArea();
            }
        }

        // Short pages may not overflow the fixed-height list, leaving no scrollbar for #onScroll
        // to ever fire on; keep pulling pages until the list overflows or pages run out.
        #fillVisibleArea() {
            if (this.#hasMore && !this.#loading && !this.#list.hidden
                    && this.#list.scrollHeight <= this.#list.clientHeight) {
                this.#loadPage(this.#currentTerm, this.#currentPage + 1, true);
            }
        }

        #search(term) {
            this.#loadPage(term, 1, false);
        }

        #onInput = () => {
            // Invalidate immediately: an old response must not render under the newly typed term
            // during the debounce interval.
            this.#cancelPendingWork();
            this.#hidden.value = '';
            // Stop scroll-loading stale results while a new search is pending.
            this.#hasMore = false;
            const term = this.#input.value;
            this.#debounceTimer = setTimeout(() => this.#search(term), DEBOUNCE_MS);
        };

        #onKeydown = (event) => {
            if (event.isComposing) {
                // Keystrokes that are part of an IME composition must not drive the widget: Enter
                // commits the composition, it doesn't pick the highlighted option.
                return;
            }
            if (this.#list.hidden) {
                if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                    event.preventDefault();
                    this.#search(this.#input.value);
                }
                return;
            }
            switch (event.key) {
                case 'ArrowDown': {
                    event.preventDefault();
                    const lastIndex = this.#options.length - 1;
                    if (this.#activeIndex >= lastIndex && this.#hasMore && !this.#loading) {
                        // Keyboard equivalent of scrolling to the bottom: pull the next page.
                        this.#loadPage(this.#currentTerm, this.#currentPage + 1, true);
                    }
                    this.#setActiveIndex(Math.min(this.#activeIndex + 1, lastIndex));
                    break;
                }
                case 'ArrowUp':
                    event.preventDefault();
                    this.#setActiveIndex(Math.max(this.#activeIndex - 1, 0));
                    break;
                case 'Enter':
                    if (this.#activeIndex >= 0 && this.#options[this.#activeIndex]) {
                        event.preventDefault();
                        this.#selectOption(this.#options[this.#activeIndex]);
                    }
                    break;
                case 'Escape':
                    this.#closeList();
                    break;
            }
        };

        // Infinite scroll: pull the next page as the user nears the bottom of the open list.
        #onScroll = () => {
            if (this.#loading || !this.#hasMore) {
                return;
            }
            const {scrollTop, clientHeight, scrollHeight} = this.#list;
            if (scrollTop + clientHeight >= scrollHeight - SCROLL_LOAD_THRESHOLD_PX) {
                this.#loadPage(this.#currentTerm, this.#currentPage + 1, true);
            }
        };

        // One delegated handler covers every page of results without allocating a closure per row.
        // "mousedown" runs before input blur, so a selection wins that race. Pressing list chrome
        // or a status row also keeps focus in the input while the user scrolls or reads feedback.
        #onListMousedown = (event) => {
            event.preventDefault();
            const item = event.target.closest('.admin-autocomplete-option');
            if (item) {
                const option = this.#options[Number(item.dataset.optionIndex)];
                if (option) {
                    this.#selectOption(option);
                }
            }
        };

        #onFocus = () => {
            // The hidden check skips the retry once the user has committed something else meanwhile.
            if (this.#pendingCurrentId && this.#hidden.value === this.#pendingCurrentId) {
                this.#lookupCurrentLabel(this.#pendingCurrentId);
            }
        };

        #onBlur = () => {
            // Deferred so a pending "mousedown" selection (see above) applies before the list is torn down.
            setTimeout(() => {
                this.#commitOrRestoreSelection();
                this.#closeList();
            }, 0);
        };

        // Typing alone must never clear the stored relation: when the user leaves the field (or
        // submits) without picking an option, restore the last committed selection. An input
        // emptied on purpose commits the cleared state instead.
        #commitOrRestoreSelection = () => {
            if (this.#hidden.value !== '') {
                return;
            }
            if (this.#input.value.trim() === '') {
                this.#selectedId = '';
                this.#selectedText = '';
            } else {
                this.#hidden.value = this.#selectedId;
                if (this.#selectedId !== '' && this.#selectedText === '') {
                    // A restored relation must never render as a blank field (its label lookup may
                    // have failed): visible and hidden state have to agree.
                    this.#selectedText = this.#fallbackLabel(this.#selectedId);
                }
                this.#input.value = this.#selectedText;
            }
        };

        #fallbackLabel(id) {
            return '#' + id;
        }

        async #prefill() {
            const current = this.#input.getAttribute('data-current');
            if (!current || this.#input.value) {
                return;
            }
            // The server usually renders the current selection's label into the input; this fetch is
            // only a fallback for templates that don't provide it.
            await this.#lookupCurrentLabel(current);
        }

        // Resolves the display label for the committed relation id. On failure the field must still
        // show that a relation is set — a blank input over a non-empty hidden id would look clear
        // while submitting the old relation — so fall back to the raw id and retry on focus.
        async #lookupCurrentLabel(current) {
            const valueAtStart = this.#input.value;
            // Claim a request seq so a late lookup can't overwrite a selection the user has
            // meanwhile made (search/select bump requestSeq).
            const seq = ++this.#requestSeq;
            const params = new URLSearchParams({id: current});
            const data = await this.#fetchOptions(`?${params}`);
            if (seq !== this.#requestSeq || this.#input.value !== valueAtStart || this.#hidden.value !== current) {
                // Superseded, or the user edited the field while the lookup was in flight (typing
                // alone doesn't bump the seq until the debounced search fires).
                return;
            }
            const option = data && !data.error ? (data.results ?? [])[0] : null;
            if (option) {
                this.#selectOption({id: option.id, text: option.text || this.#fallbackLabel(option.id)});
            } else {
                this.#pendingCurrentId = current;
                const fallback = this.#fallbackLabel(current);
                this.#input.value = fallback;
                this.#selectedText = fallback;
            }
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        for (const input of document.querySelectorAll('.admin-autocomplete-input')) {
            const url = input.getAttribute('data-options-url');
            const hidden = document.getElementById(input.getAttribute('data-target'));
            const list = document.getElementById(input.getAttribute('aria-controls'));
            if (url && hidden && list) {
                new ToOneAutocomplete({input, url, hidden, list});
            }
        }
    });
})();
