package com.bdt.kiradb;

import com.bdt.kiradb.mykdbapp.Expense;
import com.bdt.kiradb.mykdbapp.Person;

import org.apache.lucene.queryParser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CoreTest {
    Core db;

    // Gets run before each method annotated with @Test
    @Before
    public void setup() throws KiraCorruptIndexException, IOException {
        db = new Core("KiraDBIndex");
        System.out.println("Creating Index...");
        db.createIndex();
    }

    // Gets run after any method annotated with @Test
    @After
    public void teardown() throws IOException {
        db.deleteIndex();
    }

    @Test
    public void testPersonStuff() throws KiraException, IOException, ClassNotFoundException, InterruptedException {
        // try writing to the index
        Person p = new Person();
        p.setAccount("1234");
        p.setName("John Smith");
        p.setCreatedAt(new Date());
        System.out.println("Writing person...");
        db.storeObject(p);
        System.out.println("Reading person...");
        Person np = (Person) db.retrieveObjectbyPrimaryKey(p, p.getAccount());
        System.out.println("Read object: " + np.getName());
        assertNotNull("The result should not be null", np);
        assertEquals("The person's name when read is not the same as when written", p.getName(), np.getName());
    }

    @Test
    public void testExpenseStuff() throws IOException, InterruptedException, ClassNotFoundException, KiraException {

    	Expense exp1 = new Expense();
    	exp1.setCategory("Clothing");
        exp1.setDate(new Date());
        exp1.setMemo("-");
        exp1.setPayee("Marshalls");
        exp1.setTxId("14856");
        System.out.println("Writing expense 1...");
        db.storeObject(exp1);
        
        Expense exp2 = new Expense();
    	exp2.setCategory("Utilities");
        exp2.setDate(new Date());
        exp2.setMemo("garbage bill");
        exp2.setPayee("petaluma refuse and recycling");
        exp2.setTxId("11564");
        System.out.println("Writing expense... 2");
        db.storeObject(exp2);
        
        System.out.println("Reading expense... 1");
        Map<String,String> res1 = (Map<String, String>) db.retrieveObjectbyPrimaryKey(exp1, exp1.getTxId());
        assertNotNull("The result should not be null", res1);

        db.dumpDocuments(exp1.getRecordName());
        
        for (String key : res1.keySet()) {
        	System.out.println("  1 key: " + key + " value: "
        			+ res1.get(key));
        }

        System.out.println("Reading expense... 2");

        Map<String,String> res2 = (Map<String, String>) db.retrieveObjectbyPrimaryKey(exp2, exp2.getTxId());
        assertNotNull("The result should not be null", res2);
        for (String key : res2.keySet()) {
        	System.out.println("  2 key: " + key + " value: "
        			+ res2.get(key));
        }

        assertEquals("Expected txId " +  exp1.getTxId() + " but got: " + res1.get(exp1.getPrimaryKeyName()), exp1.getTxId(), res1.get(exp1.getPrimaryKeyName()));
        assertEquals("Expected txId " +  exp2.getTxId() + " but got: " + res2.get(exp2.getPrimaryKeyName()), exp2.getTxId(), res2.get(exp2.getPrimaryKeyName()));

        List<Object> q1Results = db.executeQuery(exp1, Expense.CATEGORY, "Clothing", 10, 0, Expense.DATE, true);
        assertNotNull("The result should not be null", q1Results);
        System.out.println("query 1 matched " + q1Results.size() + " records");
        for (Object id: q1Results) {
        	System.out.println("query 1 matched id: " + (String)id);

        }
        assertEquals("Category query 1 failed", (String)q1Results.get(0), exp1.getTxId());

        List<Object> q2Results = db.executeQuery(exp1, Expense.MEMO, "garbage", 10, 0, Expense.DATE, true);
        assertNotNull("The result should not be null", q2Results);
        System.out.println("query 2 matched " + q2Results.size() + " records");
        for (Object id: q2Results) {
        	System.out.println("query 2 matched id: " + (String)id);
        }
        assertEquals("Memo query 2 failed", (String)q2Results.get(0), exp2.getTxId());

    }
    
/*
    @Test
    public void testSomeResource() {
        InputStream inputStream = getClass().getResourceAsStream("/opaquedata.bin");
        Assert.assertNotNull(inputStream);
        System.out.printf("got an inputstream on some test data\n");

        // now do something useful with the inputstream...
        // ...
    }
*/

}