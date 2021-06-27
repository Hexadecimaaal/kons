import com.beust.klaxon.Klaxon
import javafx.collections.ObservableList
import java.io.File
import java.nio.file.*

data class Attrs(
  val tags : List<String>, val comment : String
)

fun Attrs() = Attrs(listOf(), "")

fun parseAttrs(file : File) = Klaxon().parse<Attrs>(file) ?: Attrs()

class FileStorage(val rootdir : Path) { inner class Item(val name : String) {

  val dataFile = rootdir.resolve(name)
  val attrFile = rootdir.resolve("$name.attr")

  fun getAttr() : Attrs = if(!Files.exists(attrFile)) Attrs() else Klaxon().parse<Attrs>(attrFile.toFile()) ?: Attrs()
}

  fun listObjects() : ObservableList<Object> = TODO()
}