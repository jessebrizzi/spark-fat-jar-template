import org.apache.spark.sql.SparkSession

object SparkJob {
  def main(args: Array[String]) {
    println("Running sample spark job")

    val spark = SparkSession.builder.
      appName("Spark Job").
      master("local[*]"). // Update when not running locally
      getOrCreate()

    val html = scala.io.Source.fromURL("https://spark.apache.org/").mkString
    val list = html.split("\n").filter(_ != "")
    val rdd = spark.sparkContext.parallelize(list)
    val count = rdd.filter(_.contains("Spark")).count()

    println(s"The number of items in the RDD: ${rdd.count()}")

    println(s"The first item in the RDD is: ${rdd.first()}")

    println(s"Found $count lines that contain the word 'Spark'")

    spark.stop()
  }
}