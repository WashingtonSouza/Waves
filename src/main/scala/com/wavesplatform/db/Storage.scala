package com.wavesplatform.db

import java.nio.charset.{Charset, StandardCharsets}

import com.google.common.primitives.{Bytes, Ints}
import com.wavesplatform.utils.forceStopApplication
import org.iq80.leveldb.{DB, WriteBatch}
import scorex.utils.ScorexLogging

import scala.util.control.NonFatal

abstract class Storage(private val db: DB) extends ScorexLogging {
  protected val Charset: Charset = StandardCharsets.UTF_8

  protected val Separator: Array[Byte] = Array[Byte](':')

  def get(key: Array[Byte]): Option[Array[Byte]] = {
    try {
      Option(db.get(key))
    } catch {
      case NonFatal(t) =>
        log.error("LevelDB get error", t)
        forceStopApplication()
        throw t
    }
  }

  def put(key: Array[Byte], value: Array[Byte]): Unit = {
    try {
      db.put(key, value)
    } catch {
      case NonFatal(t) =>
        log.error("LevelDB put error", t)
        forceStopApplication()
        throw t
    }
  }

  def createBatch(): Option[WriteBatch] = {
    try {
      Some(db.createWriteBatch())
    } catch {
      case NonFatal(t) =>
        log.error("LevelDB create batch error", t)
        forceStopApplication()
        throw t
    }
  }

  def put(key: Array[Byte], value: Array[Byte], batch: Option[WriteBatch] = None): Unit = {
    try {
      if (batch.isDefined) batch.get.put(key, value) else db.put(key, value)
    } catch {
      case NonFatal(t) =>
        log.error("LevelDB batch put error", t)
        forceStopApplication()
        throw t
    }
  }

  def delete(key: Array[Byte], batch: Option[WriteBatch] = None): Unit = {
    try {
      if (batch.isDefined) batch.get.delete(key) else db.delete(key)
    } catch {
      case NonFatal(t) =>
        log.error("LevelDB delete error", t)
        forceStopApplication()
        throw t
    }
  }

  def commit(batch: Option[WriteBatch]): Unit = {
    batch.foreach { b =>
      try {
        db.write(b)
      } catch {
        case NonFatal(t) =>
          log.error("LevelDB write batch error", t)
          forceStopApplication()
          throw t
      } finally {
        b.close()
      }
    }
  }

  protected def map(prefix: Array[Byte], stripPrefix: Boolean = true): Map[Array[Byte], Array[Byte]] = {
    val p = makePrefix(prefix)
    val it = db.iterator()
    var map = Map.empty[Array[Byte], Array[Byte]]

    try {
      it.seekToFirst()
      while (it.hasNext) {
        val e = it.peekNext()
        if (e.getKey.startsWith(p)) {
          val k = if (stripPrefix) e.getKey.drop(p.length) else e.getKey
          map = map.updated(k, e.getValue)
        }
        it.next()
      }
    } finally {
      it.close()
    }

    map
  }

  def removeEverything(b: Option[WriteBatch]): Unit

  protected def makePrefix(prefix: Array[Byte]): Array[Byte] = Bytes.concat(prefix, Separator)

  protected def makeKey(prefix: Array[Byte], key: Array[Byte]): Array[Byte] = Bytes.concat(prefix, Separator, key, Separator)

  protected def makeKey(prefix: Array[Byte], key: String): Array[Byte] = makeKey(prefix, key.getBytes(Charset))

  protected def makeKey(prefix: Array[Byte], key: Int): Array[Byte] = makeKey(prefix, Ints.toByteArray(key))
}
