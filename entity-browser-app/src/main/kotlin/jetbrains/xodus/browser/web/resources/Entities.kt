package jetbrains.xodus.browser.web.resources

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.routing.*
import jetbrains.xodus.browser.web.AppRoute
import jetbrains.xodus.browser.web.ChangeSummary
import jetbrains.xodus.browser.web.WebApplication
import mu.KLogging


class Entities(webApp: WebApplication) : AppRoute, ResourceSupport(webApp) {

    companion object : KLogging()

    private val ApplicationCall.entityId: String
        get() {
            return parameters["entityId"] ?: throw BadRequestException("entity id required")
        }

    override fun Route.install() {
        route("/dbs/{uuid}/entities") {
            get {

                val id = call.request.queryParameters["id"]?.toInt() ?: 0
                val q = call.request.queryParameters["q"]
                val offset = (call.request.queryParameters["offset"] ?: "0").toInt()
                val pageSize = (call.request.queryParameters["pageSize"] ?: "0").toInt()
                logger.debug {
                    "searching entities by typeId: $id, q [$q] with offset = $offset and pageSize = $pageSize"
                }
                if (offset < 0 || pageSize < 0) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    call.respond(
                            call.storeService.searchType(
                                    id,
                                    q,
                                    offset,
                                    if (pageSize == 0) 50 else Math.min(pageSize, 1000)
                            )
                    )
                }
            }

            get("/{entityId}") {
                call.respond(call.storeService.getEntity(call.entityId))
            }

            put("/{entityId}") {
                call.assertEditable()
                val changeSummary = call.receive(ChangeSummary::class)
                val entityId = call.entityId
                call.respond(
                        call.storeService.updateEntity(entityId, changeSummary)
                )
            }
            post {
                call.assertEditable()
                val changeSummary = call.receive(ChangeSummary::class)
                val typeId = call.request.queryParameters["typeId"]?.toInt()
                        ?: throw BadRequestException("typeId is required")

                call.respond(
                        call.storeService.newEntity(typeId, changeSummary)
                )
            }
            delete("/{entityId}") {
                call.assertEditable()
                val entityId = call.entityId
                call.respond(
                        call.storeService.deleteEntity(entityId)
                )
            }
            get("/{entityId}/blob/{blobName}") {
                val entityId = call.entityId
                val blobName = call.parameters["blobName"] ?: ""
                logger.debug { "getting entity by entity id '$entityId'" }
                call.respondOutputStream(ContentType.parse("application/octet-stream;charset=utf-8")) {
                    call.storeService.getBlob(entityId, blobName).use {
                        it.copyTo(this)
                    }
                }
            }
            get("/{entityId}/links/{linkName}") {
                val entityId = call.entityId
                val linkName = call.parameters["linkName"] ?: ""
                val offset = (call.request.queryParameters["offset"] ?: "0").toInt()
                val pageSize = (call.request.queryParameters["pageSize"] ?: "0").toInt()

                logger.debug {
                    "searching entities by typeId: $entityId, linkName [$linkName] with offset = $offset and pageSize = $pageSize"
                }
                if (offset < 0 || pageSize < 0) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    call.respond(
                            call.storeService.searchEntity(
                                    entityId,
                                    linkName,
                                    offset,
                                    if (pageSize == 0) 50 else Math.min(pageSize, 1000)
                            )
                    )
                }
            }
        }
    }
}
