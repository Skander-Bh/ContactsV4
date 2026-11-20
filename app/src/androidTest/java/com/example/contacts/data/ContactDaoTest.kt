package com.example.contacts.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentes du DAO sur une base de test dediee (contacts.db n'est pas touchee). */
@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private val testDb = "contacts_test.db"
    private lateinit var context: Context
    private lateinit var helper: ContactDbHelper
    private lateinit var dao: ContactDao

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDb)
        helper = ContactDbHelper.createForTest(context, testDb)
        dao = ContactDao(helper)
    }

    @After fun tearDown() {
        helper.close()
        context.deleteDatabase(testDb)
    }

    private fun contact(nom: String, prenom: String = "X", tel: String = "123") =
        Contact(nom = nom, prenom = prenom, telephone = tel)

    @Test fun insert_retourneUnIdEtGetByIdRelit() {
        val id = dao.insert(contact("Gharbi").copy(email = "g@x.tn", latitude = 35.0, longitude = 10.0))
        assertTrue(id > 0)
        val c = dao.getById(id)!!
        assertEquals(id, c.id); assertEquals("Gharbi", c.nom); assertEquals(35.0, c.latitude!!, 0.0)
    }

    @Test fun gpsNull_estConserveEnNull() {
        val id = dao.insert(contact("SansGps"))
        val c = dao.getById(id)!!
        assertNull(c.latitude); assertNull(c.longitude); assertNull(c.email); assertNull(c.photoUri)
    }

    @Test fun telephoneCommencantParZero_estConserve() {
        val id = dao.insert(contact("Zero", tel = "0021655111222"))
        assertEquals("0021655111222", dao.getById(id)!!.telephone)
    }

    @Test fun update_etDelete_retournentLeNombreDeLignes() {
        val id = dao.insert(contact("Avant"))
        assertEquals(1, dao.update(dao.getById(id)!!.copy(nom = "Apres")))
        assertEquals("Apres", dao.getById(id)!!.nom)
        assertEquals(0, dao.update(contact("Fantome").copy(id = 9999)))
        assertEquals(1, dao.delete(id))
        assertEquals(0, dao.delete(id))
        assertNull(dao.getById(id))
    }

    @Test fun apostrophe_neCassePasLesRequetes() {
        val id = dao.insert(contact("O'Neil", "L'Ariana"))
        assertEquals("O'Neil", dao.getById(id)!!.nom)
        assertEquals(1, dao.search("O'N").size)
        assertEquals(1, dao.search("'").size)
    }

    @Test fun injectionSql_estNeutralisee() {
        dao.insert(contact("Alpha"))
        assertEquals(0, dao.search("' OR '1'='1").size)
        assertEquals(0, dao.search("x'; DROP TABLE contacts; --").size)
        assertEquals(1, dao.getAll().size)          // la table existe toujours
    }

    @Test fun jokersLike_sontEchappes() {
        dao.insert(contact("Alpha")); dao.insert(contact("100%Sur"))
        assertEquals(1, dao.search("%").size)
        assertEquals(0, dao.search("_").size)
    }

    @Test fun tri_nomPuisPrenom_sansTenirCompteDeLaCasse() {
        dao.insert(contact("zorro", "A")); dao.insert(contact("Alpha", "b")); dao.insert(contact("alpha", "A"))
        assertEquals(listOf("A", "b", "A"), dao.getAll().map { it.prenom })
        assertEquals(listOf("alpha", "Alpha", "zorro"), dao.getAll().map { it.nom })
    }

    @Test fun search_estInsensibleALaCasse_surNomPrenomTelephone() {
        dao.insert(Contact(nom = "Trabelsi", prenom = "Sarra", telephone = "22 345 678"))
        assertEquals(1, dao.search("TRAB").size)
        assertEquals(1, dao.search("sar").size)
        assertEquals(1, dao.search("345").size)
        assertEquals(0, dao.search("zzz").size)
    }

    @Test fun contactsDeDemonstration_inseresUneSeuleFois() {
        val name = "contacts_seed_test.db"
        context.deleteDatabase(name)
        var h = ContactDbHelper.createForTest(context, name, seedDemoData = true)
        var d = ContactDao(h)
        val first = d.getAll()
        assertTrue(first.size >= 5)
        assertTrue(first.count { !it.hasPosition } >= 2)
        h.close()

        h = ContactDbHelper.createForTest(context, name, seedDemoData = true)   // 2e ouverture : onCreate non rappele
        d = ContactDao(h)
        assertEquals(first.size, d.getAll().size)
        assertNotNull(d.getAll().firstOrNull { it.nom == "O'Neil" })
        h.close()
        context.deleteDatabase(name)
    }
}
