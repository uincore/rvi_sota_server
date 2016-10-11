/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import org.scalacheck.{Arbitrary, Gen}

trait GroupInfoGenerators {

  private lazy val defaultNs: Namespace = Namespace("default")

  val genGroupName: Gen[GroupInfo.Name] = for {
    strLen <- Gen.choose(2, 100)
    name   <- Gen.listOfN[Char](strLen, Gen.alphaNumChar)
  } yield Refined.unsafeApply(name.mkString)

  val genGroupInfo: Gen[GroupInfo] =
    for {
      name <- genGroupName
      json <- SimpleJsonGenerator.simpleJsonGen
    } yield GroupInfo(Uuid.generate(), name, defaultNs, json)

  val genGroupInfoList: Gen[Seq[GroupInfo]] = {
    for {
      len   <- Gen.chooseNum(2, 5)
      infos <- Gen.listOfN(len, genGroupInfo).suchThat(l => l.length == l.distinct.length)
    } yield infos
  }

  implicit lazy val arbGroupName: Arbitrary[GroupInfo.Name] = Arbitrary(genGroupName)
  implicit lazy val arbGroupInfo: Arbitrary[GroupInfo] = Arbitrary(genGroupInfo)
}

object GroupInfoGenerators extends GroupInfoGenerators