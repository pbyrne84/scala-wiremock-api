# scala-wiremock-api

## Intro

Not the first time I have written this code. Wiremock's use of builders and static imports make 
the api fairly non-fluid to use. Scala has a lot of nice tricks to make test writing actually
fun and have an IDE discoverable API (static imports are not fun for this). Intellij has something called
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

Scenario based testing is also held together by strings which makes testing retry operations a
tad ugly. This is simply now solved by **WiremockExpectation.applyAsScenario**

**WiremockScenarioExpectationSpec** (<https://github.com/pbyrne84/scala-wiremock-api/blob/main/src/test/scala/com/github/pbyrne84/wiremockapi/remapping/WiremockScenarioExpectationSpec.scala>)
```scala
"scenario" should {
  "handle sequential calls to an api so we can mimic things like retrying" in {
    import WiremockExpectation.ops._

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

  def generateAnyExpectation(index: Int): (Json, WiremockExpectation) = {
    // language=json
    val jsonResponseBody = json.unsafeParse(s"""
        |{
        |   "body" :  "body-$index"
        |}
        |""".stripMargin)

    jsonResponseBody -> WiremockExpectation.default
      .withResponse(
        WiremockResponse.emptySuccess
          .withStatus(200 + index)
          .withResponseBody(ResponseBody.jsonBody(jsonResponseBody.spaces2))
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
It also fails in weird ways such as saying the scenario info does not match but in the log output where it is complaining
it looks identical. 

Personally, except for retries I think scenario based stuff is more for automated QA's/Pact testing. Those tests can 
get overly complicated, fail in horrible ways or worse pass in unforeseen ways. With scala and its type system we should
be able to write code and tests that communicate our intentions in a way we can verify everything is tickety-boo. ADT's
and Either for errors and tests that fail very close to the errant operation in a way that requires little debugging.

Assumption is what causes problems so having tests that operate close to the operation allows us to test assumptions 
easily. System level tests are incredibly poor for this as at an implementation level tests start to read like a magical
mystery tour as more and more things are added to them. Incoherence and tests are a recipe for not a fun time. Not 
a fun time is holey software. Not a fun time is not easily being able to tighten up a boundary with a test.

On a personal level load factor should always be taken into account.
https://wiki.c2.com/?LoadFactor

How long does it take you to write the code, how long to do the test to a high standard that covers both the safety and 
developer headache aspects. Tests are fun, they allow communication, exploration of technology and the ability to change 
tact easily. Implementation sunk cost fallacies suck.

There likely needs to be some Pact testing for things like backwards compatibility checks but if problems are found on 
that level then work out better practices in the development of the software, work smarter not harder. Working harder
also means more tiresome and being tired leads to more mistakes.

### Mechanism

There is simply a few case classes with the main one **WiremockExpectation** having **asExpectationBuilder** and 
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

Json responses also add the header. In reality much of my life dealing in microservices we are dealing with json.

```scala
case class JsonResponseBody(value: String) extends ResponseBody {
  val jsonHeader: (String, String) = "content-type" -> "application/json"
}
```





