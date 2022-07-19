package au.id.tmm.fetch.files

import fs2.{Chunk, Pull, Stream}

import scala.reflect.ClassTag

object Chunking {

  def breakByHeadings[F[_], O : ClassTag](headerPredicate: O => Boolean): fs2.Pipe[F, O, (O, Chunk[O])] = {
    case class Section(heading: O, body: Chunk[O]) {
      def concat(chunk: Chunk[O]): Section = Section(heading, Chunk.concat(List(body, chunk)))
    }

    def go(currentSection: Option[Section], remainingStream: Stream[F, O]): Pull[F, Section, Unit] =
      remainingStream.pull.uncons.flatMap {
        case Some((nextChunk, newRemainingStream)) => processChunk(currentSection, nextChunk, newRemainingStream)
        case None =>
          currentSection match {
            case Some(lastSection) => Pull.output1(lastSection)
            case None              => Pull.done
          }
      }

    @scala.annotation.tailrec
    def processChunk(
      currentSection: Option[Section],
      chunkToProcess: Chunk[O],
      remainingStream: Stream[F, O],
      sectionsProcessed: List[Section] = List.empty,
    ): Pull[F, Section, Unit] =
      chunkToProcess.indexWhere(headerPredicate) match {
        case Some(headerIndex) => {
          val (previousHeaderBody, nextSection) = chunkToProcess.splitAt(headerIndex)

          val completedSection = currentSection.map(_.concat(previousHeaderBody))

          val newHeader          = nextSection(0)
          val nextChunkToProcess = nextSection.drop(1)

          processChunk(
            currentSection = Some(Section(newHeader, Chunk.empty)),
            chunkToProcess = nextChunkToProcess,
            remainingStream,
            sectionsProcessed = sectionsProcessed ++ completedSection,
          )
        }
        case None => {
          val currentSectionForNextInvocation = currentSection.map(_.concat(chunkToProcess))

          sectionsProcessed match {
            case sectionsProcessed @ (_ :: _) =>
              Pull.output(Chunk.array(sectionsProcessed.toArray[Section])) >>
                go(currentSectionForNextInvocation, remainingStream)
            case Nil => go(currentSectionForNextInvocation, remainingStream)
          }
        }
      }

    stream =>
      go(currentSection = None, remainingStream = stream).stream.map { case Section(heading, body) =>
        (heading, body)
      }
  }

}
