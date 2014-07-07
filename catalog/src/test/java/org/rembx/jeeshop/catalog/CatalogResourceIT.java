package org.rembx.jeeshop.catalog;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rembx.jeeshop.catalog.model.Catalog;
import org.rembx.jeeshop.catalog.model.CatalogPersistenceUnit;
import org.rembx.jeeshop.catalog.model.Category;
import org.rembx.jeeshop.catalog.test.Assertions;
import org.rembx.jeeshop.catalog.test.TestCatalog;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.rembx.jeeshop.catalog.test.Assertions.assertThatCategoriesOf;

public class CatalogResourceIT {

    private CatalogResource service;

    private TestCatalog testCatalog;
    private static EntityManagerFactory entityManagerFactory;

    @BeforeClass
    public static void beforeClass(){
        entityManagerFactory = Persistence.createEntityManagerFactory(CatalogPersistenceUnit.NAME);
    }

    @Before
    public void setup(){
        testCatalog = TestCatalog.getInstance();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        service = new CatalogResource(entityManager,new CatalogItemFinder(entityManager));
    }

    @Test
    public void findCategories_shouldReturn404ExWhenCatalogNotFound() {
        try{
            service.findCategories(9999L, null);
            fail("should have thrown ex");
        }catch (WebApplicationException e){
            assertThat(e.getResponse().getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
        }
    }

    @Test
    public void findCategories_shouldReturnEmptyListWhenCatalogIsEmpty() {
        List<Category> categories = service.findCategories(testCatalog.getEmptyCatalogId(),null);
        assertThat(categories).isEmpty();
    }

    @Test
    public void findCategories_shouldNotReturnExpiredNorDisabledRootCategories() {
        List<Category> categories = service.findCategories(testCatalog.getId(),null);
        assertThatCategoriesOf(categories).areVisibleRootCategories();
    }

    @Test
    public void findAll_shouldReturnNoneEmptyList() {
        assertThat(service.findAll(null, null)).isNotEmpty();
    }

    @Test
    public void findAll_withPagination_shouldReturnNoneEmptyListPaginated() {
        List<Catalog> catalogs = service.findAll(0, 1);
        assertThat(catalogs).isNotEmpty();
        assertThat(catalogs).hasSize(1);
    }

    @Test
    public void find_withIdOfVisibleCatalog_ShouldReturnExpectedCatalog() {
        Catalog catalog = service.find(testCatalog.getId(), null);
        Assertions.assertThat(catalog).isNotNull();
        Assertions.assertThat(catalog.isVisible()).isTrue();
    }

    @Test
    public void modifyCatalog_ShouldModifyCatalogAttributesAndPreserveRootCategories() {
        Catalog catalog = service.find(testCatalog.getId(), null);

        Catalog detachedCatalogToModify = new Catalog(1L,catalog.getName());
        detachedCatalogToModify.setDescription(catalog.getDescription());

        service.modify(detachedCatalogToModify);

        assertThat(catalog.getRootCategories()).isNotEmpty();

    }

    @Test
    public void modifyUnknownCatalog_ShouldThrowNotFoundException() {

        Catalog detachedCatalogToModify = new Catalog(9999L,null);
        try {
            service.modify(detachedCatalogToModify);
            fail("should have thrown ex");
        }catch (WebApplicationException e){
            assertThat(e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}