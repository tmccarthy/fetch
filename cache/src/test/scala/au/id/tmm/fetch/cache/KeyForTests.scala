package au.id.tmm.fetch.cache

import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}

private[cache] sealed abstract class KeyForTests(val asString: String)

private[cache] object KeyForTests {
  case object ResourceA extends KeyForTests("ResourceA")

  case object ResourceB extends KeyForTests("ResourceB")
}

private[cache] sealed abstract class ValueForTests(val asString: String)

private[cache] object ValueForTests {
  def parse(asString: String): ExceptionOr[ValueForTests] = asString match {
    case ValueA.asString => Right(ValueA)
    case ValueB.asString => Right(ValueB)
    case _               => Left(GenericException(asString))
  }

  case object ValueA extends ValueForTests("ValueA")

  case object ValueB extends ValueForTests("ValueB")
}

private[cache] final class TestClient {
  def retrieve(key: KeyForTests): ValueForTests = key match {
    case KeyForTests.ResourceA => ValueForTests.ValueA
    case KeyForTests.ResourceB => ValueForTests.ValueB
  }
}
