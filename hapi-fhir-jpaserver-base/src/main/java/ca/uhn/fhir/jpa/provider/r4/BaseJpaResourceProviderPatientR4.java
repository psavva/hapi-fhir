package ca.uhn.fhir.jpa.provider.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.springframework.beans.factory.annotation.Autowired;


import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2022 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

public class BaseJpaResourceProviderPatientR4 extends JpaResourceProviderR4<Patient> {

	@Autowired
	private MemberMatcherR4Helper myMemberMatcherR4Helper;


	/**
	 * Patient/123/$everything
	 */
	@Operation(name = JpaConstants.OPERATION_EVERYTHING, idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider patientInstanceEverything(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@IdParam
			IdType theId,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT)
			UnsignedIntType theCount,

		@Description(formalDefinition="Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET)
			UnsignedIntType theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
			DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theTypes,

		@Sort
			SortSpec theSortSpec,

		RequestDetails theRequestDetails
	) {

		startRequest(theServletRequest);
		try {
			return ((IFhirResourceDaoPatient<Patient>) getDao()).patientInstanceEverything(theServletRequest, theId, theCount, theOffset, theLastUpdated, theSortSpec, toStringAndList(theContent), toStringAndList(theNarrative), toStringAndList(theFilter), toStringAndList(theTypes), theRequestDetails);
		} finally {
			endRequest(theServletRequest);
		}
	}

	/**
	 * /Patient/$everything
	 */
	@Operation(name = JpaConstants.OPERATION_EVERYTHING, idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider patientTypeEverything(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT)
			UnsignedIntType theCount,

		@Description(formalDefinition="Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET)
			UnsignedIntType theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
			DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theTypes,

		@Description(shortDefinition = "Filter the resources to return based on the patient ids provided.")
		@OperationParam(name = Constants.PARAM_ID, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<IdType> theId,

		@Sort
			SortSpec theSortSpec,

		RequestDetails theRequestDetails
	) {

		startRequest(theServletRequest);
		try {
			return ((IFhirResourceDaoPatient<Patient>) getDao()).patientTypeEverything(theServletRequest, theCount, theOffset, theLastUpdated, theSortSpec, toStringAndList(theContent), toStringAndList(theNarrative), toStringAndList(theFilter), toStringAndList(theTypes), theRequestDetails, toFlattenedPatientIdTokenParamList(theId));
		} finally {
			endRequest(theServletRequest);
		}

	}

	/**
	 * Patient/123/$summary
	 */
	@Operation(name = JpaConstants.OPERATION_SUMMARY, idempotent = true, bundleType = BundleTypeEnum.DOCUMENT)
	public Bundle patientInstanceSummary(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@IdParam
			IdType theId,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT)
			UnsignedIntType theCount,

		@Description(formalDefinition="Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET)
			UnsignedIntType theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
			DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theTypes,

		@Sort
			SortSpec theSortSpec,

		RequestDetails theRequestDetails
	) {

		startRequest(theServletRequest);
		try {
			return (
				buildSummaryFromSearch(((IFhirResourceDaoPatient<Patient>) getDao()).patientInstanceSummary(theServletRequest, theId, theCount, theOffset, theLastUpdated, theSortSpec, toStringAndList(theContent), toStringAndList(theNarrative), toStringAndList(theFilter), toStringAndList(theTypes), theRequestDetails))
				);
		} finally {
			endRequest(theServletRequest);
		}
	}

	/**
	 * /Patient/$summary
	 */
	@Operation(name = JpaConstants.OPERATION_SUMMARY, idempotent = true, bundleType = BundleTypeEnum.DOCUMENT)
	public Bundle patientTypeSummary(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT)
			UnsignedIntType theCount,

		@Description(formalDefinition="Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET)
			UnsignedIntType theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
			DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theTypes,

		@Description(shortDefinition = "Filter the resources to return based on the patient ids provided.")
		@OperationParam(name = Constants.PARAM_ID, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<IdType> theId,

		@Sort
			SortSpec theSortSpec,

		RequestDetails theRequestDetails
	) {

		startRequest(theServletRequest);
		try {
			return (
				buildSummaryFromSearch(((IFhirResourceDaoPatient<Patient>) getDao()).patientTypeSummary(theServletRequest, theCount, theOffset, theLastUpdated, theSortSpec, toStringAndList(theContent), toStringAndList(theNarrative), toStringAndList(theFilter), toStringAndList(theTypes), theRequestDetails, toFlattenedPatientIdTokenParamList(theId)))
				);
		} finally {
			endRequest(theServletRequest);
		}

	}

	private Bundle buildSummaryFromSearch(IBundleProvider searchSet) {
		Bundle bundle = new Bundle();
		List<Resource> resourceList = createResourceList(searchSet.getAllResources());
		Composition composition = buildComposition(resourceList);
		bundle.addEntry().setResource(composition);
		for (Resource resource : resourceList) {
			bundle.addEntry().setResource(resource);
		}
		return bundle;
	}

	private List<Resource> createResourceList(List<IBaseResource> iBaseResourceList) {
		List<Resource> resourceList = new ArrayList<Resource>();
		for (IBaseResource ibaseResource : iBaseResourceList) {
			resourceList.add((Resource) ibaseResource);
		}
		return resourceList;
	}

	private enum IPSSection {
		ALLERGY_INTOLERANCE,
		MEDICATION_SUMMARY,
		PROBLEM_LIST,
		IMMUNIZATIONS,
		PROCEDURES,
		MEDICAL_DEVICES,
		DIAGNOSTIC_RESULTS,
		// VITAL_SIGNS,
		// ILLNESS_HISTORY,
		// PREGNANCY,
		// SOCIAL_HISTORY,
		FUNCTIONAL_STATUS,
		PLAN_OF_CARE,
		ADVANCE_DIRECTIVES
	}

	private static final Map<IPSSection, Map<String, String>> SectionText = Map.of(
		IPSSection.ALLERGY_INTOLERANCE, Map.of("title", "Allergies and Intolerances", "code", "48765-2", "display", "Allergies and Adverse Reactions"),
		IPSSection.MEDICATION_SUMMARY, Map.of("title", "Medication", "code", "10160-0", "display", "Medication List"),
		IPSSection.PROBLEM_LIST, Map.of("title", "Active Problems", "code", "11450-4", "display", "Problem List"),
		IPSSection.IMMUNIZATIONS, Map.of("title", "Immunizations", "code", "11369-6", "display", "History of Immunizations"),
		IPSSection.PROCEDURES, Map.of("title", "Procedures", "code", "47519-4", "display", "History of Procedures"),
		IPSSection.MEDICAL_DEVICES, Map.of("title", "Medical Devices", "code", "46240-8", "display", "Medical Devices"),
		IPSSection.DIAGNOSTIC_RESULTS, Map.of("title", "Diagnostic Results", "code", "30954-2", "display", "Diagnostic Results"),
		IPSSection.VITAL_SIGNS, Map.of("title", "Vital Signs", "code", "8716-3", "display", "Vital Signs"),
		IPSSection.PREGNANCY, Map.of("title", "Pregnancy", "code", "11362-0", "display", "Pregnancy Information"),
		IPSSection.SOCIAL_HISTORY, Map.of("title", "Social History", "code", "29762-2", "display", "Social History"),
		// IPSSection.ILLNESS_HISTORY, Map.of("title", "History of Past Illness", "code", "11348-0", "display", "History of Past Illness"),
		IPSSection.FUNCTIONAL_STATUS, Map.of("title", "Functional Status", "code", "47420-5", "display", "Functional Status"),
		IPSSection.PLAN_OF_CARE, Map.of("title", "Plan of Care", "code", "18776-5", "display", "Plan of Care"),
		IPSSection.ADVANCE_DIRECTIVES, Map.of("title", "Advance Directives", "code", "42349-0", "display", "Advance Directives")
	);

	private static final Map<IPSSection, List<ResourceType>> SectionTypes = Map.of(
		IPSSection.ALLERGY_INTOLERANCE, List.of(ResourceType.AllergyIntolerance),
		IPSSection.MEDICATION_SUMMARY, List.of(ResourceType.MedicationStatement, ResourceType.MedicationRequest),
		IPSSection.PROBLEM_LIST, List.of(ResourceType.Condition),
		IPSSection.IMMUNIZATIONS, List.of(ResourceType.Immunization),
		IPSSection.PROCEDURES, List.of(ResourceType.Procedure),
		IPSSection.MEDICAL_DEVICES, List.of(ResourceType.DeviceUseStatement),
		IPSSection.DIAGNOSTIC_RESULTS, List.of(ResourceType.DiagnosticReport, ResourceType.Observation),
		IPSSection.VITAL_SIGNS, List.of(ResourceType.Observation),
		IPSSection.PREGNANCY, List.of(ResourceType.Observation),
		IPSSection.SOCIAL_HISTORY, List.of(ResourceType.Observation),
		// IPSSection.ILLNESS_HISTORY, List.of(ResourceType.Condition),
		IPSSection.FUNCTIONAL_STATUS, List.of(ResourceType.ClinicalImpression),
		IPSSection.PLAN_OF_CARE, List.of(ResourceType.CarePlan),
		IPSSection.ADVANCE_DIRECTIVES, List.of(ResourceType.Consent) 
	);

	private Composition buildComposition(List<Resource> resourceList) {
		Composition composition = new Composition();
		HashMap<IPSSection, List<Resource>> sortedResources = createIPSResourceHashMap(resourceList);
		for (IPSSection iPSSection : IPSSection.values()) {
			if (sortedResources.get(iPSSection) != null && sortedResources.get(iPSSection).size() > 0) {
				Composition.SectionComponent section = createSection(SectionText.get(iPSSection), sortedResources.get(iPSSection));
				composition.addSection(section);
			}
		}
		return composition;
	}

	private HashMap<IPSSection, List<Resource>> createIPSResourceHashMap(List<Resource> resourceList) {
		HashMap<IPSSection, List<Resource>> iPSResourceMap = new HashMap<IPSSection, List<Resource>>();
		String[] pregnancyCodes = {"82810-3", "11636-8", "11637-6", "11638-4", "11639-2", "11640-0", "11612-9", "11613-7", "11614-5", "33065-4"};		
		for (Resource resource : resourceList) {
			for (IPSSection iPSSection : IPSSection.values()) {
				if ( SectionTypes.get(iPSSection).contains(resource.getResourceType()) ) {
					if (iPSSection == "VITAL_SIGNS") {
						if (resource.getCategory().get(0).getCode() == "vital-signs") {
							iPSResourceMap.get(iPSSection).add(resource);
						}
					}
					else if (iPSSection == "PREGNANCY") {
						if (pregnancyCodes.contains(resource.getCode().getCoding().get(0).getCode())) {
							iPSResourceMap.get(iPSSection).add(resource);
						}
					}
					else if (iPSSection == "SOCIAL_HISTORY") {
						if (resource.getCategory().get(0).getCode() == "social-history") {
							iPSResourceMap.get(iPSSection).add(resource);
						}
					}
					else if (iPSSection == "DIAGNOSTIC_RESULTS") {
						if (resource.getResourceType() == "DiagnosticReport" || resource.getCategory().get(0).getCode() == "laboratory") {
							iPSResourceMap.get(iPSSection).add(resource);
						}
					}
					else {
						if ( !iPSResourceMap.containsKey(iPSSection) ) {
							iPSResourceMap.put(iPSSection, new ArrayList<Resource>());
						}
						iPSResourceMap.get(iPSSection).add(resource);
					}
				}
			}
		}

		return iPSResourceMap;
	}

	private Composition.SectionComponent createSection(Map<String, String> text, List<Resource> resources) {
		Composition.SectionComponent section = new Composition.SectionComponent();
		section.setTitle(text.get("title"))
			.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org").setCode(text.get("code")).setDisplay(text.get("display"))))
			.setText(new Narrative().setStatus(Narrative.NarrativeStatus.GENERATED).setDiv(new XhtmlNode().setValue("<div>Holder for future narrative</div>")));
		
		HashMap<ResourceType, List<Resource>> resourcesByType = new HashMap<ResourceType, List<Resource>>();
		for (Resource resource : resources) {
			if ( !resourcesByType.containsKey(resource.getResourceType()) ) {
				resourcesByType.put(resource.getResourceType(), new ArrayList<Resource>());
			}
			resourcesByType.get(resource.getResourceType()).add(resource);
		}
		for (List<Resource> resourceList : resourcesByType.values()) {
			// Cannot figure out how to add more than one entry per section once we have the method we can use this loop to do so
			// List<Reference> entry = new ArrayList<Reference>();	
			for (Resource resource : resourceList) {
				section.addEntry(new Reference(resource));
			}
		}

		return section;
	}
	
	/**
	 * /Patient/$member-match operation
	 * Basic implementation matching by coverage id or by coverage identifier. Not matching by
	 * Beneficiary (Patient) demographics in this version
	 */
	@Operation(name = ProviderConstants.OPERATION_MEMBER_MATCH, idempotent = false, returnParameters = {
		@OperationParam(name = "MemberIdentifier", typeName = "string")
	})
	public Parameters patientMemberMatch(
		javax.servlet.http.HttpServletRequest theServletRequest,

		@Description(shortDefinition = "The target of the operation. Will be returned with Identifier for matched coverage added.")
		@OperationParam(name = Constants.PARAM_MEMBER_PATIENT, min = 1, max = 1)
		Patient theMemberPatient,

		@Description(shortDefinition = "Old coverage information as extracted from beneficiary's card.")
		@OperationParam(name = Constants.PARAM_OLD_COVERAGE, min = 1, max = 1)
		Coverage oldCoverage,

		@Description(shortDefinition = "New Coverage information. Provided as a reference. Optionally returned unmodified.")
		@OperationParam(name = Constants.PARAM_NEW_COVERAGE, min = 1, max = 1)
		Coverage newCoverage,

		RequestDetails theRequestDetails
	) {
		return doMemberMatchOperation(theServletRequest, theMemberPatient, oldCoverage, newCoverage, theRequestDetails);
	}


	private Parameters doMemberMatchOperation(HttpServletRequest theServletRequest, Patient theMemberPatient,
				Coverage theCoverageToMatch, Coverage theCoverageToLink, RequestDetails theRequestDetails) {

		validateParams(theMemberPatient, theCoverageToMatch, theCoverageToLink);

		Optional<Coverage> coverageOpt = myMemberMatcherR4Helper.findMatchingCoverage(theCoverageToMatch);
		if ( ! coverageOpt.isPresent()) {
			String i18nMessage = getContext().getLocalizer().getMessage(
				"operation.member.match.error.coverage.not.found");
			throw new UnprocessableEntityException(Msg.code(1155) + i18nMessage);
		}
		Coverage coverage = coverageOpt.get();

		Optional<Patient> patientOpt = myMemberMatcherR4Helper.getBeneficiaryPatient(coverage);
		if (! patientOpt.isPresent()) {
			String i18nMessage = getContext().getLocalizer().getMessage(
				"operation.member.match.error.beneficiary.not.found");
			throw new UnprocessableEntityException(Msg.code(1156) + i18nMessage);
		}
		Patient patient = patientOpt.get();

		if (patient.getIdentifier().isEmpty()) {
			String i18nMessage = getContext().getLocalizer().getMessage(
				"operation.member.match.error.beneficiary.without.identifier");
			throw new UnprocessableEntityException(Msg.code(1157) + i18nMessage);
		}

		myMemberMatcherR4Helper.addMemberIdentifierToMemberPatient(theMemberPatient, patient.getIdentifierFirstRep());

		return myMemberMatcherR4Helper.buildSuccessReturnParameters(theMemberPatient, theCoverageToLink);
	}


	private void validateParams(Patient theMemberPatient, Coverage theOldCoverage, Coverage theNewCoverage) {
		validateParam(theMemberPatient, Constants.PARAM_MEMBER_PATIENT);
		validateParam(theOldCoverage, Constants.PARAM_OLD_COVERAGE);
		validateParam(theNewCoverage, Constants.PARAM_NEW_COVERAGE);
	}


	private void validateParam(Object theParam, String theParamName) {
		if (theParam == null) {
			String i18nMessage = getContext().getLocalizer().getMessage(
				"operation.member.match.error.missing.parameter", theParamName);
			throw new UnprocessableEntityException(Msg.code(1158) + i18nMessage);
		}
	}


	/**
	 * Given a list of string types, return only the ID portions of any parameters passed in.
	 */
	private TokenOrListParam toFlattenedPatientIdTokenParamList(List<IdType> theId) {
		TokenOrListParam retVal = new TokenOrListParam();
		if (theId != null) {
			for (IdType next: theId) {
				if (isNotBlank(next.getValue())) {
					String[] split = next.getValueAsString().split(",");
					Arrays.stream(split).map(IdType::new).forEach(id -> {
						retVal.addOr(new TokenParam(id.getIdPart()));
					});
				}
			}
		}
		return retVal.getValuesAsQueryTokens().isEmpty() ? null: retVal;
	}

	private StringAndListParam toStringAndList(List<StringType> theNarrative) {
		StringAndListParam retVal = new StringAndListParam();
		if (theNarrative != null) {
			for (StringType next : theNarrative) {
				if (isNotBlank(next.getValue())) {
					retVal.addAnd(new StringOrListParam().addOr(new StringParam(next.getValue())));
				}
			}
		}
		if (retVal.getValuesAsQueryTokens().isEmpty()) {
			return null;
		}
		return retVal;
	}

}
