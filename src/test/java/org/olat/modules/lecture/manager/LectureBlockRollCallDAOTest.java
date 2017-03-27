/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.lecture.manager;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureBlockRollCall;
import org.olat.repository.RepositoryEntry;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 20 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LectureBlockRollCallDAOTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private LectureBlockDAO lectureBlockDao;
	@Autowired
	private LectureBlockRollCallDAO lectureBlockRollCallDao;
	
	@Test
	public void createAndPersistRollCall() {
		LectureBlock lectureBlock = createMinimalLectureBlock();
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("lecturer-1");
		dbInstance.commitAndCloseSession();
		
		LectureBlockRollCall rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, id);
		dbInstance.commitAndCloseSession();
		
		Assert.assertNotNull(rollCall);
		Assert.assertNotNull(rollCall.getKey());
		Assert.assertNotNull(rollCall.getCreationDate());
		Assert.assertNotNull(rollCall.getLastModified());
		Assert.assertEquals(lectureBlock, rollCall.getLectureBlock());
		Assert.assertEquals(id, rollCall.getIdentity());	
	}
	
	@Test
	public void createAndLoadRollCall() {
		LectureBlock lectureBlock = createMinimalLectureBlock();
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("lecturer-1");
		dbInstance.commitAndCloseSession();
		
		LectureBlockRollCall rollCall = lectureBlockRollCallDao.createAndPersistRollCall(lectureBlock, id);
		dbInstance.commitAndCloseSession();
		
		LectureBlockRollCall reloadRollCall = lectureBlockRollCallDao.loadByKey(rollCall.getKey());
		
		Assert.assertNotNull(reloadRollCall);
		Assert.assertNotNull(reloadRollCall.getKey());
		Assert.assertNotNull(reloadRollCall.getCreationDate());
		Assert.assertNotNull(reloadRollCall.getLastModified());
		Assert.assertEquals(rollCall, reloadRollCall);
		Assert.assertEquals(lectureBlock, reloadRollCall.getLectureBlock());
		Assert.assertEquals(id, reloadRollCall.getIdentity());	
	}


	private LectureBlock createMinimalLectureBlock() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		LectureBlock lectureBlock = lectureBlockDao.createLectureBlock(entry);
		lectureBlock.setStartDate(new Date());
		lectureBlock.setEndDate(new Date());
		lectureBlock.setTitle("Hello lecturers");
		return lectureBlockDao.update(lectureBlock);
	}
}
