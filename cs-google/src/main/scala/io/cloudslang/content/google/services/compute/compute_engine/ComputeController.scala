package io.cloudslang.content.google.services.compute.compute_engine

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.compute.model.Operation

import scala.concurrent.ExecutionContext.Implicits.global
import io.cloudslang.content.google.services.compute.compute_engine.operations.ZoneOperationService

import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by marisca on 4/9/2017.
  */
object ComputeController {
  def awaitSuccessOperation(httpTransport: HttpTransport, jsonFactory: JsonFactory, credential: Credential, projectId: String,
                            zone: String, operation: Operation, timeout: Long, pollingInterval: Long): Operation =
    Await.result(updateOperationProgress(httpTransport, jsonFactory, credential, projectId, zone, operation, pollingInterval), if (timeout == 0) Inf else timeout seconds)

  def updateOperationProgress(httpTransport: HttpTransport, jsonFactory: JsonFactory, credential: Credential, projectId: String,
                              zone: String, operation: Operation, pollingInterval: Long): Future[Operation] =
    Future {
      Stream.continually {
        Thread.sleep(pollingInterval)
        ZoneOperationService.get(httpTransport, jsonFactory, credential, projectId, zone, operation.getName)
      }
        .filter(_.getProgress == 100)
        .head
    }
}
