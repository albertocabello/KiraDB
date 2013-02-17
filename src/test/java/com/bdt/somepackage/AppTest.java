package com.bdt.somepackage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AppTest {

    // Gets run before each method annotated with @Test
    @Before
    public void setup() {

    }

    // Gets run after any method annotated with @Test
    @After
    public void teardown() {

    }

    @Test
    public void testApp1() {
        Assert.assertTrue(true);
        System.out.printf("hello test 1\n");
    }

    @Test
    public void testApp2() {
        Assert.assertTrue(true);
        System.out.printf("hello test 2\n");
    }
}