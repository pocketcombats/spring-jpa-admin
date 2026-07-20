package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.MethodPropertyReader;
import com.pocketcombats.admin.core.property.MethodPropertyWriter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedFormFieldAccessorTest {

    private EmbeddedFormFieldAccessor accessor;

    @BeforeEach
    void setUp() throws Exception {
        List<EmbeddedFormFieldProperty> properties = List.of(
                property("street", "Street"),
                property("city", "City"),
                property("zip", "Zip"),
                property("unit", "Unit")
        );

        accessor = new EmbeddedFormFieldAccessor(
                new DefaultConversionService(),
                "address",
                Address.class,
                properties,
                beanReader(Order.class, "address"),
                beanWriter(Order.class, "address")
        );
    }

    @Test
    void readValueOfNullEmbeddableYieldsNullProperties() {
        Map<String, Object> values = accessor.readValue(new Order());

        // Key order is not part of the contract — the template iterates _properties, not this map.
        assertEquals(Set.of("street", "city", "zip", "unit"), values.keySet());
        values.forEach((name, value) -> assertNull(value, name));
    }

    @Test
    void readValueExposesExistingProperties() {
        Order order = new Order();
        order.setAddress(new Address("Baker Street", "London", "NW1", 221));

        Map<String, Object> values = accessor.readValue(order);

        assertEquals("Baker Street", values.get("street"));
        assertEquals("London", values.get("city"));
        assertEquals("NW1", values.get("zip"));
        assertEquals(221, values.get("unit"));
    }

    @Test
    void modelAttributesExposePropertiesInDeclarationOrder() {
        List<?> properties = (List<?>) accessor.getModelAttributes(new Order()).get("_properties");

        assertEquals(
                List.of("street", "city", "zip", "unit"),
                properties.stream().map(p -> ((EmbeddedFormFieldProperty) p).name()).toList()
        );
    }

    @Test
    void bindInstantiatesEmbeddableAndConvertsValues() {
        Order order = new Order();
        BindingResult bindingResult = bind(order, data("Baker Street", "London", "NW1", "221"));

        assertFalse(bindingResult.hasErrors());
        assertNotNull(order.getAddress());
        assertEquals("Baker Street", order.getAddress().getStreet());
        assertEquals("London", order.getAddress().getCity());
        assertEquals("NW1", order.getAddress().getZip());
        assertEquals(221, order.getAddress().getUnit());
    }

    @Test
    void bindKeepsEmbeddableNullWhenNoValueSubmitted() {
        Order order = new Order();
        BindingResult bindingResult = bind(order, data("", "", "", ""));

        assertFalse(bindingResult.hasErrors());
        // No non-empty value was submitted, so no empty embeddable is instantiated.
        assertNull(order.getAddress());
    }

    @Test
    void bindInstantiatesEmbeddableAsSoonAsOnePropertyHasValue() {
        Order order = new Order();
        BindingResult bindingResult = bind(order, data("", "London", "", ""));

        assertFalse(bindingResult.hasErrors());
        assertNotNull(order.getAddress());
        assertNull(order.getAddress().getStreet());
        assertEquals("London", order.getAddress().getCity());
    }

    @Test
    void bindClearsPropertyOnEmptySubmittedValue() {
        Order order = new Order();
        order.setAddress(new Address("Old", "Old", "000", 1));

        BindingResult bindingResult = bind(order, data("New Street", "", "000", ""));

        assertFalse(bindingResult.hasErrors());
        assertEquals("New Street", order.getAddress().getStreet());
        assertNull(order.getAddress().getCity());
        assertNull(order.getAddress().getUnit());
    }

    @Test
    void bindRejectsUnconvertibleValueWithoutFailingOtherProperties() {
        Order order = new Order();
        BindingResult bindingResult = bind(order, data("Baker Street", "London", "NW1", "not-a-number"));

        assertTrue(bindingResult.hasFieldErrors("address.unit"));
        assertEquals(
                "spring-jpa-admin.validation.constraints.ValidValue.message",
                bindingResult.getFieldError("address.unit").getCode()
        );
        // Remaining properties still bound despite the rejected one.
        assertEquals("Baker Street", order.getAddress().getStreet());
        assertNull(order.getAddress().getUnit());
    }

    private BindingResult bind(Order order, MultiValueMap<String, String> data) {
        BindingResult bindingResult = new BeanPropertyBindingResult(order, "order");
        accessor.bind("model-field-address", order, data, bindingResult);
        return bindingResult;
    }

    private static MultiValueMap<String, String> data(String street, String city, String zip, String unit) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("model-field-address.street", street);
        data.add("model-field-address.city", city);
        data.add("model-field-address.zip", zip);
        data.add("model-field-address.unit", unit);
        return data;
    }

    private static EmbeddedFormFieldProperty property(String name, String label) throws Exception {
        AdminModelPropertyReader reader = beanReader(Address.class, name);
        return new EmbeddedFormFieldProperty(
                name, label, reader.getJavaType(), reader, beanWriter(Address.class, name)
        );
    }

    private static MethodPropertyReader beanReader(Class<?> type, String name) throws Exception {
        PropertyDescriptor pd = new PropertyDescriptor(name, type);
        return new MethodPropertyReader(name, pd.getReadMethod());
    }

    private static MethodPropertyWriter beanWriter(Class<?> type, String name) throws Exception {
        PropertyDescriptor pd = new PropertyDescriptor(name, type);
        return new MethodPropertyWriter(name, pd.getWriteMethod());
    }

    static class Order {
        private @Nullable Address address;

        public @Nullable Address getAddress() {
            return address;
        }

        public void setAddress(@Nullable Address address) {
            this.address = address;
        }
    }

    static class Address {
        private String street;
        private String city;
        private String zip;
        private Integer unit;

        public Address() {
        }

        public Address(String street, String city, String zip, Integer unit) {
            this.street = street;
            this.city = city;
            this.zip = zip;
            this.unit = unit;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

        public Integer getUnit() {
            return unit;
        }

        public void setUnit(Integer unit) {
            this.unit = unit;
        }
    }
}
