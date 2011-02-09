package org.apache.log4j.net;

import org.junit.Test;

public class ValidateTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyString_Invalid_Empty() {
        Validate.notEmptyString("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyString_Invalid_Null() {
        Validate.notEmptyString(null, null);
    }

    @Test
    public void testNotEmptyString_Valid() {
        Validate.notEmptyString("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPositiveInteger_Invalid() {
        Validate.positiveInteger(-1, null);
    }

    @Test
    public void testPositiveInteger_Valid() {
        Validate.positiveInteger(0, null);
    }
}
