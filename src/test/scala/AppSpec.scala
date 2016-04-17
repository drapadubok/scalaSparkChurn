import org.scalatest.FlatSpec
import churn.ChurnData._

class AppSpec extends FlatSpec {

  "bool2double(true)" should "equal 1.0" in {
    assert(bool2double(true)==1.0)
  }

  "bool2double(false)" should "equal 0.0" in {
    assert(bool2double(false)==0.0)
  }

  "getIdxDropColumns" should "return indices of columns to drop" in {
    assert(getIdxDropColumns(Array("1", "2", "3"), Array("2", "3")) sameElements Array(1, 2))
  }

}


