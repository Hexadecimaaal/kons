import arrow.core.*
import com.beust.klaxon.Klaxon
import java.io.File
import java.nio.file.*
import kotlin.io.path.*

data class Attrs(
  val tags : List<String> = listOf(),
  val comment : String = "",
  val dataManaged : Boolean = false,
  val dataFilePath : Option<String> = None
)

fun parseAttrs(file : File) = Klaxon().parse<Attrs>(file) ?: Attrs()
class FileStorage(val rootdir : Path) {
  inner class Item(val name : String) {

    private val attrFile : Path = rootdir.resolve("$name.attr")

    fun exists() : Boolean = Files.exists(attrFile)

    fun getAttr() : Attrs =
      if(!Files.exists(attrFile)) Attrs()
      else Klaxon().parse<Attrs>(attrFile.toFile()) ?: Attrs()

    fun putAttr(attrs : Attrs) {
      attrFile.writeText(Klaxon().toJsonString(attrs))
    }

    fun delete() = Files.delete(attrFile)

    override fun equals(other : Any?) =
      if(other is Item) other.name == this.name
      else false

  }


  fun get(name : String) : Option<Item> {
    val i = Item(name)
    return if(i.exists()) Some(i)
    else None
  }

  fun list() : List<Item> =
    rootdir.listDirectoryEntries("*.attr").map { Item(it.fileName.pathString.removeSuffix(".attr")) }

  fun put(name : String, attrs : Attrs) =
    Item(name).putAttr(attrs)

  fun fresh() : Item {
    val l = list().filter { it.name.toIntOrNull() != null }
    val i = l.maxByOrNull { it.name.toInt() }
    return Item(((i?.name?.toInt() ?: 0) + 1).toString())
  }

  fun post(attrs : Attrs) : String {
    val i = fresh()
    put(i.name, attrs)
    return i.name
  }

  fun delete(name : String) = Item(name).delete()

  //fun listObjects() : ObservableList<Object> = TODO()
}

class TagStorage(val rootdir : Path) {
  inner class Item(val name : String) {
    private val folder : Path = rootdir.resolve(name)

    fun exists() : Boolean = Files.exists(folder)

    fun getFileStorage() = FileStorage(folder)

    fun delete() = Files.delete(folder)

    override fun equals(other : Any?) =
      if(other is Item) other.name == this.name
      else false
  }

  fun get(name : String) : Option<Item> {
    val i = Item(name)
    return if(i.exists()) Some(i)
    else None
  }

  fun list() : List<Item> =
    rootdir.listDirectoryEntries("*").map { Item(it.fileName.pathString) }

  fun put(name : String, attrs : Attrs) =
    Item(name).putAttr(attrs)

  fun fresh() : Item {
    val l = list().filter { it.name.toIntOrNull() != null }
    val i = l.maxByOrNull { it.name.toInt() }
    return Item(((i?.name?.toInt() ?: 0) + 1).toString())
  }

  fun post(attrs : Attrs) : String {
    val i = fresh()
    put(i.name, attrs)
    return i.name
  }

  fun delete(name : String) = Item(name).delete()

}


