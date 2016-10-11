JSON handling
=============

This readme is created to clarify the gotcha in the reading and writing between **json** and **case class**

In play, there are two ways to specify how to read and write from a json to a case class
- Formatter
...This object contain both a reader and writer for the case class
- Reader/Writer
...These are defined specifically to only reade or write

The issue highlighted here is in regards to when implicit formatter as well as reader/writer is used in unison. 

When jsons are read/write the `implicit formatter` will take precedence over the `implicit reader/writer`. 
i.e. if an `implicit` formatter is declared then calls to the reader/writer must be made explicitly. 

e.g.in the below example
```scala
object Demo{
    implicit val reader = new Reads[Demo] {...}
    implicit val formatter = Json.format[Demo]
}
```
Despite being explicitly defined and specified as implicit, the reader is ignored unless called explicitly. 

It is therefore recommended in presence of an implicit formatter the reader and writer are **not** declared as `implicit` as this could lead to false assumptions being made.


