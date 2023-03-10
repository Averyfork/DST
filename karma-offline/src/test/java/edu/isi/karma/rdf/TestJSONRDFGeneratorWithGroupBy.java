/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.rdf;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.kr2rml.mapping.R2RMLMappingIdentifier;


/**
 * @author dipsy
 * 
 */
public class TestJSONRDFGeneratorWithGroupBy extends TestJSONRDFGenerator{

	private static Logger logger = LoggerFactory.getLogger(TestJSONRDFGeneratorWithGroupBy.class);
	

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		// Add the models in
		R2RMLMappingIdentifier modelIdentifier = new R2RMLMappingIdentifier(
				"groupby-top-model", getTestResource(
						 "groupby/groupby-top-model.ttl"));
		rdfGen.addModel(modelIdentifier);
		modelIdentifier = new R2RMLMappingIdentifier(
				"groupby-nested-model", getTestResource(
						 "groupby/groupby-nested-model.ttl"));
		rdfGen.addModel(modelIdentifier);
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGroupByNested() {
		try {

			executeBasicJSONTest("groupby/groupby-nested.json", "groupby-nested-model", false, 43);
		} catch (Exception e) {
			logger.error("testGroupByNested failed:", e);
			fail("Exception: " + e.getMessage());
		}
	}
	
	@Test
	public void testGroupByTop() {
		try {
			executeBasicJSONTest("groupby/groupby-top.json", "groupby-top-model", false, 15);
		} catch (Exception e) {
			logger.error("testGroupByTop failed:", e);
			fail("Exception: " + e.getMessage());
		}
	}

}
