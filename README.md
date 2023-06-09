# scala-wiremock-api

[![scala-wiremock-api Scala version support](https://index.scala-lang.org/pbyrne84/scala-wiremock-api/scala-wiremock-api/latest.svg)](https://index.scala-lang.org/pbyrne84/scala-wiremock-api/scala-wiremock-api)


## Usage
```scala
"uk.org.devthings" %% "scala-wiremock-api" % "{version-from-above}" % Test
```


## Wiremock logging tip

Note when using wiremock is very handy to have a plain logger setup for the test so wiremock's output is readable.

e.g.
```
-------------------------------
| Closest stub                                             | Request                                                  |
-----------------------------------------------------------------------------------------------------------------------
                                                           |
PATCH                                                      | DELETE               <<<<< HTTP method does not match
[regex] .*                                                 | /                    <<<<< null. URLs must start with a /
                                                           |
                                                           |
-----------------------------------------------------------------------------------------------------------------------
```

Also, it is very useful to have a wiremock instance per api we need to fake. If this is not done, then things like output
 on the nearest match stop making any sense neutering the ability of wiremock to be friendly.

## Intro

Not the first time I have written this code. Wiremock's use of builders and static imports make 
the api fairly non-fluid to use. Scala has a lot of nice tricks to make test writing actually
fun and have an IDE-discoverable API (static imports are not fun for this). Intellij has something called
smart complete (https://www.jetbrains.com/idea/guide/tips/smart-completion/) which when combined with tight
signatures can help a lot.

There is also the issue that we sometimes want to do a loose expectation to guarantee errant 404
responses are not accidentally achieving the same result in the test. 404 is often used for 
non-authorized as explicit non-authorized responses can leak data. 501 (not implemented) would 
probably be a better default response as 500's lead to hard failures and not accidental 
successes. 

A loose expectation (such as no json body check) but a tight verification can lead to
a better informative test failure as well, this is hard to do in wiremock as there are 2
different builders so any work has to be  duplicated which leads to just tight
verifications being used as software engineers are lazy and adverse to duplicated effort. 
I include myself in that statement.

Scenario based testing is also held together by strings which make testing retry operations a
tad ugly. This is simply now solved by **WiremockExpectation.applyAsScenario**

**WireMockScenarioExpectationSpec** (<https://github.com/pbyrne84/scala-wiremock-api/blob/main/src/test/scala/uk/org/devthings/scala/wiremockapi/remapping/WiremockScenarioExpectationSpec.scala>)
```scala
"scenario" should {
  "handle sequential calls to an api so we can mimic things like retrying" in {
    import WireMockExpectation.ops._

    val (expectedJsonResponseBody1, expectation1) = generateAnyExpectation(0)
    val (expectedJsonResponseBody2, expectation2) = generateAnyExpectation(1)
    val (expectedJsonResponseBody3, expectation3) = generateAnyExpectation(2)

    List(
      expectation1,
      expectation2,
      expectation3
    ).applyAsScenario(wireMock.server)

    val request = basicRequest.get(uri"${wireMock.baseRequestUrl}/")
    val response1 = request.send(sttpBackend)
    val response2 = request.send(sttpBackend)
    val response3 = request.send(sttpBackend)

    response1.code.code shouldBe 200
    response2.code.code shouldBe 201
    response3.code.code shouldBe 202

    response1.body.flatMap(json.parse) shouldBe Right(expectedJsonResponseBody1)
    response2.body.flatMap(json.parse) shouldBe Right(expectedJsonResponseBody2)
    response3.body.flatMap(json.parse) shouldBe Right(expectedJsonResponseBody3)

  }

  def generateAnyExpectation(index: Int): (Json, WireMockExpectation) = {
    // language=json
    val jsonResponseBody = json.unsafeParse(s"""
        |{
        |   "body" :  "body-$index"
        |}
        |""".stripMargin)

    jsonResponseBody -> WireMockExpectation
      .willRespondStatus(200 + index)
      .willRespondWithBody(
         jsonResponseBody.spaces2.asJsonResponse
      )
  }
}
```

```scala
applyAsScenario
```

Is an extension method that uses a **ScenarioInfoGenerator** to auto generate the scenario info
```scala
  val default: ScenarioInfoGenerator = new ScenarioInfoGenerator {
    override val scenarioName: String = "auto-generated-scenario"

    def createScenarioInfo(scenarioName: String, index: Int, maxIndex: Int): ScenarioInfo = {
      if (index == 0) {
        ScenarioInfo(scenarioName = scenarioName, expectedCurrentState = Scenario.STARTED, nextState = createName(1))
      } else {
        ScenarioInfo(
          scenarioName = scenarioName,
          expectedCurrentState = createName(index),
          nextState = createName(index + 1)
        )
      }
    }

    private def createName(index: Int): String =
      s"wiremock-step-$index"

  }
```

Wiremock scenarios are not thread safe. There is a link in the spec **WiremockScenarioExpectationSpec** to the issue. 
It also fails in weird ways, such as saying the scenario info does not match but in the log output where it is complaining
it looks identical. 

Except for retries, I think scenario based stuff is more for automated QA's/Pact testing. Those tests can 
get overly complicated, fail in horrible ways or worse pass in unforeseen ways. With Scala and its type system, we should
be able to write code and tests that communicate our intentions in a way we can verify everything is tickety-boo. ADT's
and Either for errors and tests that fail very close to the errant operation in a way that requires little debugging.

Assumption is what causes problems, so having tests that operate close to the operation allows us to test assumptions 
easily. System level tests are incredibly poor for this as at an implementation level, tests start to read like a magical
mystery tour as more and more things are added to them. Incoherence and tests are a recipe for not a fun time. Not 
a fun time is holey software. Not a fun time is not easily being able to tighten up a boundary with a test.

On a personal level load factor should always be taken into account.
https://wiki.c2.com/?LoadFactor

How long does it take you to write the code, how long to do the test to a high standard that covers both the safety and 
developer headache aspects. Tests are fun. They increase communication, easily allow the exploration of technology and 
enable the ability to change tact more easily. Implementation sunk cost fallacies suck.

There likely needs to be some Pact testing for things like backward compatibility checks, but if problems are found on 
that level, then better practices in the development of the software are needed, work smarter not harder. Working harder
also means more tiresome and being tired leads to more mistakes.

## Mechanism

There is simply a few case classes with the main one **WireMockExpectation** having **asExpectationBuilder** and 
**asVerificationBuilder** operations. This enables the ability to do the main loose expectation then use the copy 
operation/fluent methods to create a tight operation for the verification.

By default, saying you expect a json body will add the content type header expectation. Doing this manually all the time
is boring, so it just makes things nicer to use. Boring can also lead to creativity in the wrong places (this project?).

```scala
BodyValueExpectation.equalsJson("{}")
```

versus

```scala
BodyValueExpectation.equalsJson("{}").withDisabledAutoHeader
```

Json responses also add the header. In reality, much of my life dealing in microservices we are dealing with json.

```scala
case class JsonResponseBody(value: String) extends ResponseBody {
  val jsonHeader: (String, String) = "content-type" -> "application/json"
}
```

### Example LooseWireMockExpectationTightVerificationExampleSpec
<https://github.com/pbyrne84/scala-wiremock-api/blob/main/src/test/scala/uk/org/devthings/scala/wiremockapi/remapping/LooseWireMockExpectationTightVerificationExampleSpec.scala>

```scala
class LooseWireMockExpectationTightVerificationExampleSpec extends BaseSpec {

  before {
    reset()
  }

  // Simple example where 404's could lead to confusion.
  "Using a loose expectation and tight verification" should {
    import WireMockExpectation.ops._

    "create a test we can be sure is correct easily" in {

      // language=json
      val expectedBody1 =
        """
          |{"payload-1" : "value-1"}
          |""".stripMargin

      // language=json
      val expectedBody2 =
        """
          |{"payload-2" : "value-2"}
          |""".stripMargin

      // We do not care about header checks and body checks at this point, body checks are pretty troublesome
      // as people can use the string expectation instead of the json expectation meaning the expectation is not
      // json formatting safe.
      val firstCallLooseExpectation =
        WireMockExpectation.willRespondOk
          .expectsMethod(Post) // we could skip the method here and add it later but method is pretty hard to fail on
          .expectsUrl("/api-path-1".asUrlPathEquals)

      val secondCallLooseExpectation =
        WireMockExpectation.willRespondOk
          .expectsMethod(Post) // we could skip the method here and add it later but method is pretty hard to fail on
          .expectsUrl("/api-path-2".asUrlPathEquals)

      wireMock.stubExpectation(firstCallLooseExpectation)
      wireMock.stubExpectation(secondCallLooseExpectation)

      val result = for {
        _ <- callServer(path = "api-path-1", body = expectedBody1)
        _ <- callServer(path = "api-path-2", body = expectedBody2)
      } yield true

      import uk.org.devthings.scala.wiremockapi.remapping.WireMockExpectation.ops._

      val paramExpectation = ("param1" -> "paramValue1").asEqualTo

      // Do verifies first as the shouldBe failure will be non-informative in this case
      wireMock.verify(
        firstCallLooseExpectation
          .expectsBody(BodyValueExpectation.equalsJson(expectedBody1))
          .expectsQueryParam(paramExpectation)
      )

      wireMock.verify(
        secondCallLooseExpectation
          .expectsBody(BodyValueExpectation.equalsJson(expectedBody2))
          .expectsQueryParam(paramExpectation)
      )

      // A None can be cause by either call failing
      result shouldBe Some(true)

    }
  }

  /**
    * An auth check could just return a boolean/empty list etc. And auth checks can chain.
    *
    * @param path
    * @param body
    * @return
    */
  def callServer(path: String, body: String): Option[Boolean] = {
    import sttp.client3._
    val response = basicRequest
      .post(uri"${wireMock.baseRequestUrl}/$path?param1=paramValue1")
      .contentType("application/json") // this is auto-checked by BodyValueExpectation.equalsJson
      .body(body)
      .send(sttpBackend)

    if (response.code.code == 200) {
      Some(true)
    } else {
      None
    }
  }
}

```




