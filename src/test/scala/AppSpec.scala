import org.scalatest.FlatSpec
import churn.ChurnData._

class AppSpec extends FlatSpec {

  "bool2int(true)" should "equal 1" in {
    assert(bool2int(true)==1)
  }

  "bool2int(false)" should "equal 0" in {
    assert(bool2int(false)==0)
  }

  "getDropColumns" should "return indices of columns to drop" in {
    assert(getDropColumns(Array("1", "2", "3"), Array("2", "3")) sameElements Array(1, 2))
  }
}


