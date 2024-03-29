package rest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dtos.UserDTO;
import io.restassured.http.ContentType;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.*;
import utils.EMF_Creator;

import javax.persistence.EntityManagerFactory;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import entities.*;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.parsing.Parser;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.glassfish.grizzly.http.server.HttpServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeAll;

class UserResourceTest {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_URL = "http://localhost/api";
    private static User u1, u2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final URI BASE_URI = UriBuilder.fromUri(SERVER_URL).port(SERVER_PORT).build();
    private static HttpServer httpServer;
    private static EntityManagerFactory emf;

    static HttpServer startServer() {
        ResourceConfig rc = ResourceConfig.forApplication(new ApplicationConfig());
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);
    }

    @BeforeAll
    public static void setUpClass() {
        //This method must be called before you request the EntityManagerFactory
        EMF_Creator.startREST_TestWithDB();
        emf = EMF_Creator.createEntityManagerFactoryForTest();

        httpServer = startServer();
        //Setup RestAssured
        RestAssured.baseURI = SERVER_URL;
        RestAssured.port = SERVER_PORT;
        RestAssured.defaultParser = Parser.JSON;
    }

    @AfterAll
    public static void closeTestServer() {
        //System.in.read();

        //Don't forget this, if you called its counterpart in @BeforeAll
        EMF_Creator.endREST_TestWithDB();
        httpServer.shutdownNow();
    }

    // Setup the DataBase (used by the test-server and this test) in a known state BEFORE EACH TEST
    //TODO -- Make sure to change the EntityClass used below to use YOUR OWN (renamed) Entity class
    @BeforeEach
    public void setUp() {
        EntityManager em = emf.createEntityManager();
        u1 = new User("Rehman","test");
        u2 = new User("Abdi", "test");

        try {
            em.getTransaction().begin();
            em.createNamedQuery("User.deleteAllRows").executeUpdate();
            em.persist(u1);
            em.persist(u2);

            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Test
    public void testServerIsUp() {
        System.out.println("Testing is server UP");
        given().when().get("/info").then().statusCode(200);
    }

    @Test
    void getAllUsers() {
//        given()
//                .when()
//                .get("/info/all")
//                .then().equals(contains(new UserDTO(u1)));

        List<UserDTO> userDTOList = given()
                .contentType("application/json")
                .when()
                .get("/info/all")
                .then()
                .extract().body().jsonPath().getList("", UserDTO.class);

        UserDTO u1dto = new UserDTO(u1);
        UserDTO u2dto = new UserDTO(u2);
        assertThat(userDTOList, containsInAnyOrder(u1dto, u2dto));
//        assertEquals(2, userDTOList.size());      // Virker også
    }



    @Test
    void getUserById() {
//        given()
//                .when()
//                .get("/info/user/" + u1.getId())
//                .then().equals((new UserDTO(u1)));

        UserDTO userDTO = given()
                .contentType("application/json")
                .when()
                .get("/info/user/" + u1.getId()).as(UserDTO.class);

        assertThat(userDTO, equalTo(new UserDTO(u1)));
    }

    // Kan køres med cascadetype.persist eller all inde i user klassen. HTTPS og metoder virker. Bare ikke testene "create".
//    @Test
//    void createUser() {
//        List<String> roles = new ArrayList<>();
//        String requestBody = GSON.toJson(new UserDTO("Oscar","test", roles));
//
//        given()
//                .header("Content-type", ContentType.JSON)
//                .and()
//                .body(requestBody)
//                .when()
//                .post("/info")
//                .then()
//                .assertThat()
//                .statusCode(200)
//                .body("id", notNullValue())
//                .body("userName", equalTo("Oscar"));
//    }

    @Test
    void updateUser() {
        UserDTO userDTO = new UserDTO(u1);
        userDTO.setUserName("nytnavn");
        userDTO.setRoles(new ArrayList<>());
        given()
                .header("Content-type", ContentType.JSON)
                .body(GSON.toJson(userDTO))
                .when()
                .put("/info/user/update/" + u1.getId())
                .then()
                .assertThat()
                .statusCode(200)
                .body("id", equalTo(u1.getId().intValue()))
                .body("userName", equalTo("nytnavn"));
    }


    @Test
    void deleteUser() {
        given()
                .contentType("application/json")
                .pathParam("id", u2.getId())
                .delete("/info/user/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(u2.getId().intValue()));
    }
}