package com.actelion.research.spirit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.RevisionType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.audit.DifferenceItem.ChangeType;
import com.actelion.research.spiritcore.business.audit.DifferenceList;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.IOUtils;

public class RevisionTest extends AbstractSpiritTest {

	@BeforeClass
	public static void init() throws Exception {
		initDemoExamples(user);
	}

	@Test
	public void testQueryRevision() throws Exception {
		List<Revision> revs = DAORevision.queryRevisions(new RevisionQuery(null, "S-00001", null, null, true, false, false, false, false));
		Assert.assertTrue(revs.size()>0);

		revs = DAORevision.queryRevisions(new RevisionQuery(null, "S-00001", null, null, false, true, true, true, false));
		Assert.assertTrue(revs.size()>0);

		revs = DAORevision.queryRevisions(new RevisionQuery(null, "S-00001", null, null, true, true, true, true, false));
		Assert.assertTrue(revs.size()>0);

		revs = DAORevision.queryRevisions(new RevisionQuery(null, null, null, null, true, true, true, true, true));
		Assert.assertTrue(revs.size()>0);

		revs = DAORevision.queryRevisions(new RevisionQuery(null, "S-00000", null, null, true, true, true, true, true));
		Assert.assertTrue(revs.size()==0);
	}

	@Test
	public void testRevertInsert() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(sampleId);
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		List<Revision> revisions = DAORevision.getLastRevisions(b);
		Assert.assertEquals(1, revisions.size());

		Revision rev = revisions.get(0);
		Revision rev2 = DAORevision.getRevision(rev.getRevId());
		Assert.assertEquals(rev.toString(), rev2.toString());
		Assert.assertEquals(RevisionType.ADD, rev.getRevisionType());
		DAORevision.revert(rev, user);

		// Load Revisions
		JPAUtil.clearAll();
		Assert.assertNull(DAOBiosample.getBiosample(sampleId));
	}

	@Test
	public void testRevertUpdate() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(sampleId);
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		// Update
		b.setComments("New comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		Assert.assertEquals("New comments", DAOBiosample.getBiosample(sampleId).getComments());

		// Load Revisions
		JPAUtil.clearAll();
		List<Revision> revisions = DAORevision.getLastRevisions(b);
		Assert.assertEquals(2, revisions.size());

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.MOD, rev.getRevisionType());
		DAORevision.revert(rev, user);
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(b);
		Assert.assertEquals("Old comments", b.getComments());

	}

	@Test
	public void testRevertDelete() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(b.getSampleId());
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		// Delete
		DAOBiosample.deleteBiosamples(Collections.singleton(b), user);
		Assert.assertNull(DAOBiosample.getBiosample(sampleId));

		// Load Revisions
		List<Revision> revisions = DAORevision.getLastRevisions(b);
		Assert.assertEquals(2, revisions.size());

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		DAORevision.revert(rev, user);
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(sampleId + " not found", b);
		Assert.assertEquals("Old comments", b.getComments());

	}

	@Test
	public void testRevertCombo() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(b.getSampleId());
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		Result r = new Result(DAOTest.getTest("Weighing"));
		r.setBiosample(b);
		r.setElb("MyTestElb");
		r.setValue(DAOTest.getTest("Weighing").getOutputAttributes().get(0), "10");
		DAOResult.persistResults(Collections.singletonList(r), user);

		// Delete
		EntityManager session = JPAUtil.getManager();
		try {
			session.getTransaction().begin();
			DAOResult.deleteResults(session, Collections.singleton(r), user);
			DAOBiosample.deleteBiosamples(session, Collections.singleton(b), user);
			session.getTransaction().commit();
		} catch (Exception e) {
			session.getTransaction().rollback();
		}

		Assert.assertNull(DAOBiosample.getBiosample(sampleId));

		// Load Revisions
		JPAUtil.clearAll();
		List<Revision> revisions = DAORevision.getLastRevisions(b);
		Assert.assertEquals(2, revisions.size());

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		DAORevision.revert(rev, user);
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(sampleId + " not found", b);
		Assert.assertEquals("Old comments", b.getComments());

	}
	/*
	@Test
	public void testRevertCombo2() throws Exception {
		ExchangeTest.initDemoExamples(user);

		Study study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		List<Revision> revisions = DAORevision.getLastRevisions(study);
		Assert.assertTrue(revisions.size() > 0);
		JPAUtil.pushEditableContext(user);

		study.setNotes("Test Combo2");
		DAOStudy.persistStudies(Collections.singleton(study), user);

		JPAUtil.closeFactory();
		// Load Revisions
		study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		Assert.assertEquals("Test Combo2", study.getNotes());
		revisions = DAORevision.getLastRevisions(study);
		Assert.assertTrue(revisions.size() > 0);

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.MOD, rev.getRevisionType());
		DAORevision.revert(rev, user);
		study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		Assert.assertNotNull(study);

		//Close Factory
		JPAUtil.closeFactory();

		// Delete results (rev 1)
		List<Result> res = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue(res.size() > 0);
		DAOResult.deleteResults(res, user);
		res = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertEquals(0, res.size());

		// Delete biosamples (rev 2)
		List<Biosample> bios = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue(bios.size() > 0);
		DAOBiosample.deleteBiosamples(bios, user);

		//Make sure deletion worked
		bios = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertEquals(0, bios.size());

		//Close Factory
		JPAUtil.closeFactory();

		//Load revisions and revert
		revisions = DAORevision.queryRevisions(new RevisionQuery());
		System.out.println("RevisionTest.testRevertCombo2() "+revisions);
		rev = revisions.get(0);
		//		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		Assert.assertTrue(rev.getBiosamples().size() > 0);
		DAORevision.revert(rev, user);

		rev = revisions.get(1);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		Assert.assertTrue(rev.getResults().size() > 0);
		DAORevision.revert(rev, user);

	}
	 */

	@Test
	public void testDocuments() throws Exception {
		// System.setProperty("show_sql", "true");
		File f = IOUtils.createTempFile("test_", ".txt");
		f.getParentFile().mkdirs();
		IOUtils.bytesToFile("Some file content".getBytes(), f);

		// Create Biotype
		Biotype biotype = new Biotype();
		biotype.setName("TestDocRev");
		biotype.setCategory(BiotypeCategory.LIBRARY);
		biotype.getMetadata().add(new BiotypeMetadata("file", DataType.D_FILE));
		biotype.getMetadata().add(new BiotypeMetadata("large", DataType.LARGE));
		DAOBiotype.persistBiotype(biotype, user);

		// Create Biosample
		Biosample b1 = new Biosample(biotype);
		b1.setMetadataDocument(biotype.getMetadata("file"), new Document(f));
		b1.setMetadataValue("large", "test1");
		DAOBiosample.persistBiosamples(Collections.singleton(b1), user);

		// Update Biosample
		b1.setMetadataValue("large", "test2");
		// b1.setDoc(Collections.singletonMap(biotype.getMetadata("file"), new
		// Document(f)));
		DAOBiosample.persistBiosamples(Collections.singleton(b1), user);

		// Retrieve Sample
		// JPAUtil.clear();
		b1 = DAOBiosample.getBiosample(b1.getSampleId());
		// Assert.assertNotNull(b1.getDoc());
		Assert.assertNotNull(b1.getMetadataDocument(biotype.getMetadata("file")));
		Assert.assertEquals("test2", b1.getMetadataValue(biotype.getMetadata("large")));

		// Retrieve versions
		List<Revision> revs = DAORevision.getLastRevisions(b1);
		Assert.assertEquals(2, revs.size());

		Biosample r1 = revs.get(0).getBiosamples().get(0);
		Biosample r2 = revs.get(1).getBiosamples().get(0);
		Assert.assertNotNull("File not retrievable in revision", r1.getMetadataDocument(biotype.getMetadata("file")));
		Assert.assertEquals("Invalid text", "test2", r1.getMetadataValue("large"));
		Assert.assertEquals("Invalid text", "test1", r2.getMetadataValue("large"));

	}

	@Test
	public void testRevertDocs() throws Exception {
		Biotype biotype = new Biotype("TestRevertDoc");
		biotype.setCategory(BiotypeCategory.PURIFIED);
		biotype.getMetadata().add(new BiotypeMetadata("doc", DataType.D_FILE));
		DAOBiotype.persistBiotype(biotype, user);

		//Create a sample with a doc and delete it
		Biosample b = new Biosample(biotype);
		b.setMetadataDocument(biotype.getMetadata("doc"), new Document("Test", "bytes".getBytes()));
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		DAOBiosample.deleteBiosamples(Collections.singleton(b), user);



		//Test revision
		List<Revision> revs = DAORevision.getLastRevisions(b);
		Assert.assertEquals(2, revs.size());
		Assert.assertEquals(RevisionType.DEL, revs.get(0).getRevisionType());

		Biosample r1 = revs.get(0).getBiosamples().get(0);
		Assert.assertEquals(biotype, r1.getBiotype());
		Assert.assertEquals("Test", new String(r1.getMetadataDocument(biotype.getMetadata("doc")).getFileName()));
		Assert.assertEquals("bytes", new String(r1.getMetadataDocument(biotype.getMetadata("doc")).getBytes()));

		//Restore
		DAORevision.restore(Collections.singleton(r1), user);
		List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSampleIdOrContainerIds(r1.getSampleId()), user);
		Assert.assertEquals(biotype, biosamples.get(0).getBiotype());
		Assert.assertEquals("Test", new String(biosamples.get(0).getMetadataDocument(biotype.getMetadata("doc")).getFileName()));
		Assert.assertEquals("bytes", new String(biosamples.get(0).getMetadataDocument(biotype.getMetadata("doc")).getBytes()));
	}


	/**
	 * Tests the RevisionQuery
	 * @throws Exception
	 */
	@Test
	public void testRevisionQuery() throws Exception {
		Study study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-1"), user).get(0);

		//Study changes
		RevisionQuery query = new RevisionQuery();
		query.setStudyIdFilter(study.getStudyId());
		query.setStudies(true);
		query.setSamples(false);
		query.setResults(false);
		List<Revision> revs = DAORevision.queryRevisions(query);
		Assert.assertTrue(revs.size()>0);
		System.out.println("RevisionTest.testRecentChanges() "+revs);

		//samples changes
		query.setStudies(false);
		query.setSamples(true);
		query.setResults(false);
		revs = DAORevision.queryRevisions(query);
		Assert.assertTrue(revs.size()>0);
		System.out.println("RevisionTest.testRecentChanges() "+revs);

		//results changes
		query.setStudies(false);
		query.setSamples(false);
		query.setResults(true);
		revs = DAORevision.queryRevisions(query);
		Assert.assertTrue(revs.size()>0);
		System.out.println("RevisionTest.testRecentChanges() "+revs);

	}


	/**
	 * Tests that differences are computed, and the reason for change is saved
	 * @throws Exception
	 */
	@Test
	public void testAuditDifferenceOnStudyAndSamples() throws Exception {
		DBAdapter.getInstance().setAuditSimplified(false);
		JPAUtil.pushEditableContext(user);
		Study s = new Study();
		DAOStudy.persistStudies(Collections.singleton(s), user);
		JPAUtil.popEditableContext();

		JPAUtil.pushEditableContext(user);
		List<Biosample> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			Biosample b = new Biosample(DAOBiotype.getBiotype("Animal"));
			b.setInheritedStudy(s);
			list.add(b);
		}
		DAOBiosample.persistBiosamples(list, user);
		JPAUtil.popEditableContext();

		RevisionQuery q = new RevisionQuery();
		q.setStudyIdFilter(s.getStudyId());
		List<Revision> revs = DAORevision.queryRevisions(q);

		Assert.assertEquals(2, revs.size());
		System.out.println("RevisionTest.testAuditDifferenceOnStudyAndSamples() "+revs.get(0).getDifference());
		System.out.println("RevisionTest.testAuditDifferenceOnStudyAndSamples() "+revs.get(1).getDifference());
		DBAdapter.getInstance().setAuditSimplified(true);

	}
	/**
	 * Tests that differences are computed, and the reason for change is saved
	 * @throws Exception
	 */
	@Test
	public void testAuditDifferenceOnSample() throws Exception {

		JPAUtil.pushEditableContext(user);
		Biosample b = new Biosample(DAOBiotype.getBiotype("Animal"));
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		JPAUtil.popEditableContext();

		JPAUtil.pushEditableContext(user);
		b.setMetadataValue("Type", "Rat");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		JPAUtil.popEditableContext();

		JPAUtil.pushEditableContext(user);
		b.setMetadataValue("Type", "Mice");
		JPAUtil.setReasonForChange(MiscUtils.mapOf("Type", "Error of type"));
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		JPAUtil.popEditableContext();

		JPAUtil.pushEditableContext(user);
		b.setMetadataValue("Sex", "M");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		JPAUtil.popEditableContext();

		List<Revision> revs = DAORevision.getLastRevisions(b);
		Assert.assertTrue(revs.size()==4);
		Assert.assertTrue(revs.get(0).getDifference().serialize().contains("Sex"));
		Assert.assertTrue(revs.get(1).getDifference().serialize().contains("Type"));
		Assert.assertTrue(revs.get(2).getDifference().serialize().contains("Type"));
		Assert.assertTrue(revs.get(2).getDifference().get(0).getChangeType()==ChangeType.MOD);
		Assert.assertTrue(revs.get(3).getDifference().get(0).getChangeType()==ChangeType.ADD);

		Assert.assertEquals("", revs.get(0).getReason());
		Assert.assertEquals("Error of type", revs.get(1).getReason());
		Assert.assertEquals("", revs.get(2).getReason());
		Assert.assertEquals("", revs.get(3).getReason());

	}

	/**
	 * Tests that differences are computed, and the reason for change is saved
	 * @throws Exception
	 */
	@Test
	public void testAuditDifferenceOnBatch() throws Exception {

		Study s = DAOStudy.getStudies().get(0);

		//initial save
		List<Biosample> biosamples = new ArrayList<>();
		for (int i=0;i<20;i++) {
			Biosample b = new Biosample(DAOBiotype.getBiotype("Animal"));
			biosamples.add(b);
		}
		DAOBiosample.persistBiosamples(biosamples, user);

		//update
		for (Biosample b : biosamples) {
			b.setMetadataValue("Type", "Rat");
			b.setMetadataValue("Sex", "M");
			b.setAttachedStudy(s);
		}
		DAOBiosample.persistBiosamples(biosamples, user);

		//delete
		DAOBiosample.deleteBiosamples(biosamples, user);

		List<Revision> revs = DAORevision.queryRevisions(new RevisionQuery());
		Assert.assertEquals(20, revs.get(0).getBiosamples().size());
		Assert.assertEquals(20, revs.get(1).getBiosamples().size());
		Assert.assertEquals(20, revs.get(2).getBiosamples().size());

		Assert.assertEquals(RevisionType.DEL, revs.get(0).getType());
		Assert.assertEquals(RevisionType.MOD, revs.get(1).getType());
		Assert.assertEquals(RevisionType.ADD, revs.get(2).getType());
		Assert.assertEquals(s, revs.get(0).getStudy());
		Assert.assertEquals(s, revs.get(1).getStudy());
		Assert.assertEquals(null, revs.get(2).getStudy());

		System.out.println("RevisionTest.testAuditDifferenceOnBatch() 0="+revs.get(0).getDifference());
		System.out.println("RevisionTest.testAuditDifferenceOnBatch() 1="+revs.get(1).getDifference());
		System.out.println("RevisionTest.testAuditDifferenceOnBatch() 2="+revs.get(2).getDifference());

	}

	@Test
	public void testAuditDifferenceOnProperties() throws Exception {
		JPAUtil.pushEditableContext(user);
		SpiritProperties.getInstance().setValue(PropertyKey.SYSTEM_HOMEDAYS, "14");
		SpiritProperties.getInstance().saveValues();
		JPAUtil.popEditableContext();

		JPAUtil.pushEditableContext(user);
		SpiritProperties.getInstance().setValue(PropertyKey.SYSTEM_HOMEDAYS, "7");
		SpiritProperties.getInstance().saveValues();
		JPAUtil.popEditableContext();

		//Query lastChange
		Pair<SpiritProperty, DifferenceList> lastChange = DAORevision.getLastChange(new SpiritProperty(PropertyKey.SYSTEM_HOMEDAYS.getKey(), "5"));
		Assert.assertEquals("system.home.days=7", lastChange.getFirst().toString());
		Assert.assertEquals("Property;system.home.days;;system.home.days;5;7;;1", lastChange.getSecond().serialize());
		System.out.println("RevisionTest.testAuditDifferenceOnProperties() "+lastChange);

		lastChange = DAORevision.getLastChange(new SpiritProperty(PropertyKey.SYSTEM_HOMEDAYS.getKey(), "7"));
		Assert.assertEquals("system.home.days=7", lastChange.getFirst().toString());
		Assert.assertEquals("", lastChange.getSecond().serialize());


		//Query revision
		RevisionQuery q = new RevisionQuery();
		List<Revision> revs = DAORevision.queryRevisions(q);

		Assert.assertEquals(1, revs.get(0).getSpiritProperties().size());

		Assert.assertEquals("Property;system.home.days;;system.home.days;7;14;;1", revs.get(0).getDifference().serialize());

	}

}
