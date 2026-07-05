package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pt.isel.domain.SecurityPrincipal
import pt.isel.http.dto.CreateEvidenceRequestDto
import pt.isel.http.dto.UpdateEvidenceRequestDto
import pt.isel.services.AccidentEvidenceService
import pt.isel.services.Failure
import pt.isel.services.Success

@RestController
class AccidentEvidenceController(
    private val evidenceService: AccidentEvidenceService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Cases.EVIDENCE)
    fun getEvidenceForCase(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = evidenceService.getEvidenceByCaseId(caseId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Cases.EVIDENCE)
    fun createEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
        @RequestBody request: CreateEvidenceRequestDto,
    ): ResponseEntity<*> {
        val access =
            when (val result = accessControl.authorizeCase(currentUser, caseId)) {
                is CaseAccessResult.Authorized -> result
                is CaseAccessResult.Rejected -> return result.response
            }

        return when (
            val result =
                evidenceService.createEvidence(
                    caseId = caseId,
                    evidenceType = request.evidenceType,
                    evidenceDescription = request.evidenceDescription,
                    uploadedBy = access.currentUser.userId,
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/evidence/${result.value.evidenceId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Evidence.GET_BY_ID)
    fun getEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
    ): ResponseEntity<*> =
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> ResponseEntity.ok(access.evidence.toOutputDto())
            is EvidenceAccessResult.Rejected -> access.response
        }

    @PutMapping(Uris.Evidence.UPDATE_BY_ID)
    fun updateEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
        @RequestBody request: UpdateEvidenceRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> Unit
            is EvidenceAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                evidenceService.updateEvidence(
                    evidenceId = evidenceId,
                    evidenceType = request.evidenceType,
                    evidenceDescription = request.evidenceDescription,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Evidence.DELETE_BY_ID)
    fun deleteEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> Unit
            is EvidenceAccessResult.Rejected -> return access.response
        }

        return when (val result = evidenceService.deleteEvidence(evidenceId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Evidence.IMAGE)
    fun getImageEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> Unit
            is EvidenceAccessResult.Rejected -> return access.response
        }

        return when (val result = evidenceService.getImageEvidenceByEvidenceId(evidenceId)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Evidence.IMAGE, consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImageEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("vehicleId") vehicleId: Int,
        @RequestParam("metadata", required = false) metadata: String?,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> Unit
            is EvidenceAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                evidenceService.uploadImageEvidence(
                    evidenceId = evidenceId,
                    vehicleId = vehicleId,
                    bytes = file.bytes,
                    contentType = file.contentType,
                    metadata = metadata,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Evidence.IMAGE_CONTENT)
    fun getImageEvidenceContent(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> Unit
            is EvidenceAccessResult.Rejected -> return access.response
        }

        return when (val result = evidenceService.getImageContent(evidenceId)) {
            is Success ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.value.contentType))
                    .body(result.value.bytes)

            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Evidence.IMAGE)
    fun deleteImageEvidence(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable evidenceId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeEvidence(currentUser, evidenceId)) {
            is EvidenceAccessResult.Authorized -> Unit
            is EvidenceAccessResult.Rejected -> return access.response
        }

        return when (val result = evidenceService.deleteImageEvidence(evidenceId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
