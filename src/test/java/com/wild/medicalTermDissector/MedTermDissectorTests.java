package com.wild.medicalTermDissector;


import com.wild.medicalTermDissector.affix.Affix;
import com.wild.medicalTermDissector.affix.AffixRepository;
import com.wild.medicalTermDissector.affix.AffixService;
import com.wild.medicalTermDissector.medicalTerms.MedTerm;
import com.wild.medicalTermDissector.medicalTerms.MedTermRepository;
import com.wild.medicalTermDissector.medicalTerms.MedTermService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class MedTermDissectorTests {

  private final MedTermRepository medTermRepository;
  private final AffixRepository affixRepository;

  @Autowired
  MedTermDissectorTests(MedTermRepository medTermRepository, AffixRepository affixRepository) {
    this.medTermRepository = medTermRepository;
    this.affixRepository = affixRepository;
  }


  @BeforeAll
  @Test
  public static void checkMariadb() {
    StringBuffer output = new StringBuffer();
    Process p;
    try {
      p = Runtime.getRuntime().exec("docker ps");
      p.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println(output);
    assertTrue(output.toString().contains("mariadb"));
  }

  @Test
  public void getMedTermsTest() {
    Boolean returnedList = true;
    MedTermService medTermService = new MedTermService(medTermRepository);
    final List<MedTerm> medTerms = medTermService.getMedTerm();
//    System.out.println("\n" + medTerms + "\n");
    if (medTerms == null) {
      returnedList = false;
    }
    assertTrue(returnedList);
  }

  @Test
  public void addMedTermsSuccess() {
    MedTermService medTermService = new MedTermService(medTermRepository);
    List<MedTerm> medTerms = List.of(
      new MedTerm("hyperventilation", "abnormally rapid breathing"),
      new MedTerm("hypertrophy", "increase in the size of an organ due to an increase in the size of its cells"));
    for (MedTerm medTerm : medTerms) {
      medTermService.addMedTerm(medTerm);
    }
    assertTrue(medTermService.getMedTerm("hyperventilation").isPresent());
  }

  @Test
  public void addMedTermSuccess() {
    MedTermService medTermService = new MedTermService(medTermRepository);
    final MedTerm medTerm = new MedTerm("hyperplasia", "the enlargement of an organ or tissue ");
    medTermService.addMedTerm(medTerm);
    assertTrue(medTermService.getMedTerm("hyperplasia").isPresent());
  }

  @Test
  public void findByNameStartsWithSuccess() {
    MedTermService medTermService = new MedTermService(medTermRepository);
    final MedTerm medTerm = new MedTerm("hyperstasis", "something here");
    medTermService.addMedTerm(medTerm);
    final List<MedTerm> medTermsList = medTermService.getMedTerms("hy");
//    System.out.println("\n" + medTermsList + "\n");
    assertFalse(medTermsList.isEmpty());
  }

  @Test
  public void updateMedTermSuccess() {
    String name = "hypoglycemia";
    final MedTermService medTermService = new MedTermService(medTermRepository);
    final MedTerm medTerm = new MedTerm(name, "below normal levels of sugar in blood.");
    medTermService.addMedTerm(medTerm);
    final Optional<MedTerm> optionalMedTerm = medTermService.getMedTerm(name);
    try {
      optionalMedTerm.get().getId();
    } catch (Exception err) {
      System.out.println("\nMedical term doesn't exist.\n");
      fail();
    }
    final Long termId = medTermService.getMedTerm(name).get().getId();
    medTermService.updateMedTerm(termId, name, "Lack of sugar in blood");
    assertTrue(medTermService.getMedTerm(name).isPresent());
  }

  @Test
  public void deleteMedTermSuccess() {
    Boolean testFailed = false;
    final MedTermService medTermService = new MedTermService(medTermRepository);
    final MedTerm medTerm = new MedTerm("removeme", "This term must be removed.");
    medTermService.addMedTerm(medTerm);
    try {
      medTermService.deleteMedTerm(medTerm.getId());
    } catch (Exception err) {
    }
    final Optional<MedTerm> optionalMedTerm = medTermRepository.findById(medTerm.getId());
    if (optionalMedTerm.isPresent()) {
      testFailed = true;
    }
    assertFalse(testFailed);
  }

  //FIXME:
  // 1: Test this with 5 other words, and then also test with a word that I don't have complete or exact affix for.
  // For example: hypovolemia. I don't have affix for 'vol'. what should your function do, if it cannot find a affix
  // which matches or even closely matches?  It's possible vol really is a variation of ole, but that would mean emia
  // is mixed with ole. Find out if that is a common thing with medical terms, or if that rarely or never happens.
  // According to the internet hypovolemia is a decrease of blood volume. So vol = volume. Which is where because I
  // cannot find any affix for vol. So maybe I'll make one. It's probably not a medical affix, it could actually be a
  // normal english affix. That might be more common of a problem. Best solution if my function cannot find a matching
  // affix for vol, is to return a exception which states that word has affixes not visible in the database. If that
  // happens, whatever function that called this function should offer to guide user to adding the medical term and
  // it's appropriate affixes to the database.
  @Test
  public void dissectSuccess() {
    String term = "hypoglycemia";
    AffixService affixService = new AffixService(affixRepository);
    List<Affix> dissectedParts = affixService.dissect(term);
    System.out.println(dissectedParts.get(0).getAffix());
    System.out.println("meaning: " + dissectedParts.get(0).getMeaning());
    System.out.println(dissectedParts.get(1).getAffix());
    System.out.println("meaning: " + dissectedParts.get(1).getMeaning());
    System.out.println(dissectedParts.get(2).getAffix());
    System.out.println("meaning: " + dissectedParts.get(2).getMeaning());
    System.out.println(dissectedParts.get(0).getExamples());
    assertEquals("hyp(o)-", dissectedParts.get(0).getAffix());
    assertEquals("below normal", dissectedParts.get(0).getMeaning()); //"hypo-"
    assertEquals("sugar", dissectedParts.get(1).getMeaning()); //"glyc-"
    assertEquals("blood condition (Am. Engl.),blood", dissectedParts.get(2).getMeaning()); //"-emia"
//    assertEquals("hypovolemia, hypoxia", dissectedParts.get(0).getExamples());    // For some reason, all my examples columns have weird newlines and I can't get rid of them in database. Figure that out then this test will work.
  }

  @Test
  public void testMissingAffix() {
    // Ideally this should return "hypo: lack of, vol: null, emia: blood"
    String term = "hypovolemia";
    AffixService affixService = new AffixService(affixRepository);
    List<Affix> dissectedParts = affixService.dissect(term);
    System.out.println(dissectedParts.get(0).getAffix());   // hypo-
    System.out.println("meaning: " + dissectedParts.get(0).getMeaning()); // below normal
    System.out.println(dissectedParts.get(1).getAffix());   // "vol"
    System.out.println("meaning: " + dissectedParts.get(1).getMeaning()); // null
    System.out.println(dissectedParts.get(2).getAffix());
    System.out.println("meaning: " + dissectedParts.get(2).getMeaning());
    assertEquals("hyp(o)-", dissectedParts.get(0).getAffix());
    assertEquals("below normal", dissectedParts.get(0).getMeaning());
    assertEquals(null, dissectedParts.get(1).getMeaning());
    assertEquals("blood condition (Am. Engl.),blood", dissectedParts.get(2).getMeaning());
  }


  @Test
  public void testTwoLetterParentheses() {
    String term = "analgesic";
    // "-alge(si)" is a affix with double letter parentheses. (COMPLETE)

    // Furthermore an-, and ana- are both real affixes, but only one will work in this instance.
    // How can algorithm choose the correct one every time?

    // Also 'an-' actually appears twice in affix list because it has two different meanings based on context.
    // In these instances it should return both affixes and let the user choose which one makes more sense.
    // After the user chooses the one that makes the most sense, it should send me a message and then I will make the
    // algorithm automatically choose that option for that word from then on.

    // Also algesic is not a affix that exists. (c) is missing. How should it handle that?
    // c is only one letter so it shouldn't even be considered a dissected part. (COMPLETE)
    AffixService affixService = new AffixService(affixRepository);
    List<Affix> dissectedParts = affixService.dissect(term);
    System.out.println(dissectedParts.get(0).getAffix());
    System.out.println("meaning: " + dissectedParts.get(0).getMeaning());
    System.out.println(dissectedParts.get(1).getAffix());
    System.out.println("meaning: " + dissectedParts.get(1).getMeaning());
    System.out.println(dissectedParts.get(2).getAffix());
    System.out.println("meaning: " + dissectedParts.get(2).getMeaning());
    System.out.println(dissectedParts.get(0).getExamples());
    assertEquals("an-", dissectedParts.get(0).getAffix());
    assertEquals("anus", dissectedParts.get(0).getMeaning()); //"an-"
    assertEquals("pain", dissectedParts.get(1).getMeaning()); //"alge(si)"
    assertEquals(null, dissectedParts.get(2)); //"c"
  }

  @Test
  public void testRootWord() {
    String term = "antibody"; // "-alge(si)" is a affix with double letter parentheses.
    // Also algesic is not a affix that exists. (c) is missing. How should it handle that?
    // c is only one letter so it shouldn't even be considered a dissected part.
    AffixService affixService = new AffixService(affixRepository);
    List<Affix> dissectedParts = affixService.dissect(term);
    System.out.println(dissectedParts.get(0).getAffix());
    System.out.println("meaning: " + dissectedParts.get(0).getMeaning());
    System.out.println(dissectedParts.get(1).getAffix());
    System.out.println("meaning: " + dissectedParts.get(1).getMeaning());
    System.out.println(dissectedParts.get(2).getAffix());
    System.out.println("meaning: " + dissectedParts.get(2).getMeaning());
    System.out.println(dissectedParts.get(0).getExamples());
    assertEquals("an-", dissectedParts.get(0).getAffix());
    assertEquals("anus", dissectedParts.get(0).getMeaning()); //"an-"
    assertEquals("pain", dissectedParts.get(1).getMeaning()); //"alge(si)"
    assertEquals(null, dissectedParts.get(2)); //"c"
  }

}

/*
  @Test
  public void customMysqlQueryTest() {

  }
*/

/* //FIXME: This is a good test for normal Java, but in spring it won't work due to the fact spring tests require very special handling, especially due to spring dependency injection preventing normal unit and integration tests from working.
  @Test   // Source: https://examples.javacodegeeks.com/core-java/java-11-standardized-http-client-api-example/
  public void httpPostTest() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create("http://localhost:8080/api/student"))
      .timeout(Duration.ofSeconds(15))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString("name=betsy&email=betsy@mail.com&dob=1992-08-06"))  // HttpRequest.BodyPublishers.ofFile(Paths.get("file.json")) This is how to get from a file instead of string.
      .build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.discarding());
    assertTrue(response.statusCode() == 201, "Status Code is not Created");
  }


  @Test
    public void httpGetTest() {
    HttpClient client = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(10))
//      .proxy(ProxySelector.of(new InetSocketAddress("www-proxy.com", 8080)))
      .authenticator(Authenticator.getDefault())
      .build();
  }
*/

// This is only if you are using the persistence.xml, which this project doesn't use.
//  @Test
//  public void mysqlTest() {
//    StudentService studentService = new StudentService(studentRepository);
////    test mysql stuff using EntityManager here:
//    String name = "billy";
//    LocalDate dob = LocalDate.of(1835, 04, 23);
//    String email = "billy@mail.com";
//
//    Student student = new Student();
//    EntityManagerFactory emf = Persistence.createEntityManagerFactory("test1");
//    EntityManager em = emf.createEntityManager();
//    em.getTransaction().begin();
////    em.createNativeQuery("SELECT * FROM student");
//    student.setName(name);
//    student.setDob(dob);
//    student.setEmail(email);
//    em.persist(student);
//    em.getTransaction().commit();
//    em.close();
//    emf.close();
//
//    assertTrue(studentService.getStudent(email).isPresent());
//  }

//  @Test
//  public void GetStudentFail() {
//
//  }
