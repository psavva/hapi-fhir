package ca.uhn.fhir.jpa.provider.r4.patient;

import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

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

public class PatientSummary {

	private enum IPSSection {
		ALLERGY_INTOLERANCE,
		MEDICATION_SUMMARY,
		PROBLEM_LIST,
		IMMUNIZATIONS,
		PROCEDURES,
		MEDICAL_DEVICES,
		DIAGNOSTIC_RESULTS,
		VITAL_SIGNS,
		// ILLNESS_HISTORY,
		PREGNANCY,
		SOCIAL_HISTORY,
		FUNCTIONAL_STATUS,
		PLAN_OF_CARE,
		ADVANCE_DIRECTIVES
	}

	private static final Map<IPSSection, Map<String, String>> SectionText = Map.ofEntries(
		Map.entry(IPSSection.ALLERGY_INTOLERANCE, Map.of("title", "Allergies and Intolerances", "code", "48765-2", "display", "Allergies and Adverse Reactions")),
		Map.entry(IPSSection.MEDICATION_SUMMARY, Map.of("title", "Medication List", "code", "10160-0", "display", "Medication List")),
		Map.entry(IPSSection.PROBLEM_LIST, Map.of("title", "Problem List", "code", "11450-4", "display", "Problem List")),
		Map.entry(IPSSection.IMMUNIZATIONS, Map.of("title", "History of Immunizations", "code", "11369-6", "display", "History of Immunizations")),
		Map.entry(IPSSection.PROCEDURES, Map.of("title", "History of Procedures", "code", "47519-4", "display", "History of Procedures")),
		Map.entry(IPSSection.MEDICAL_DEVICES, Map.of("title", "Medical Devices", "code", "46240-8", "display", "Medical Devices")),
		Map.entry(IPSSection.DIAGNOSTIC_RESULTS, Map.of("title", "Diagnostic Results", "code", "30954-2", "display", "Diagnostic Results")),
		Map.entry(IPSSection.VITAL_SIGNS, Map.of("title", "Vital Signs", "code", "8716-3", "display", "Vital Signs")),
		Map.entry(IPSSection.PREGNANCY, Map.of("title", "Pregnancy Information", "code", "11362-0", "display", "Pregnancy Information")),
		Map.entry(IPSSection.SOCIAL_HISTORY, Map.of("title", "Social History", "code", "29762-2", "display", "Social History")),
		// Map.entry(IPSSection.ILLNESS_HISTORY, Map.of("title", "History of Past Illness", "code", "11348-0", "display", "History of Past Illness")),
		Map.entry(IPSSection.FUNCTIONAL_STATUS, Map.of("title", "Functional Status", "code", "47420-5", "display", "Functional Status")),
		Map.entry(IPSSection.PLAN_OF_CARE, Map.of("title", "Plan of Care", "code", "18776-5", "display", "Plan of Care")),
		Map.entry(IPSSection.ADVANCE_DIRECTIVES, Map.of("title", "Advance Directives", "code", "42349-0", "display", "Advance Directives"))
	);

	private static final Map<IPSSection, List<ResourceType>> SectionTypes = Map.ofEntries(
		Map.entry(IPSSection.ALLERGY_INTOLERANCE, List.of(ResourceType.AllergyIntolerance)),
		Map.entry(IPSSection.MEDICATION_SUMMARY, List.of(ResourceType.MedicationStatement, ResourceType.MedicationRequest)),
		Map.entry(IPSSection.PROBLEM_LIST, List.of(ResourceType.Condition)),
		Map.entry(IPSSection.IMMUNIZATIONS, List.of(ResourceType.Immunization)),
		Map.entry(IPSSection.PROCEDURES, List.of(ResourceType.Procedure)),
		Map.entry(IPSSection.MEDICAL_DEVICES, List.of(ResourceType.DeviceUseStatement)),
		Map.entry(IPSSection.DIAGNOSTIC_RESULTS, List.of(ResourceType.DiagnosticReport, ResourceType.Observation)),
		Map.entry(IPSSection.VITAL_SIGNS, List.of(ResourceType.Observation)),
		Map.entry(IPSSection.PREGNANCY, List.of(ResourceType.Observation)),
		Map.entry(IPSSection.SOCIAL_HISTORY, List.of(ResourceType.Observation)),
		// Map.entry(IPSSection.ILLNESS_HISTORY, List.of(ResourceType.Condition)),
		Map.entry(IPSSection.FUNCTIONAL_STATUS, List.of(ResourceType.ClinicalImpression)),
		Map.entry(IPSSection.PLAN_OF_CARE, List.of(ResourceType.CarePlan)),
		Map.entry(IPSSection.ADVANCE_DIRECTIVES, List.of(ResourceType.Consent))
	);

	private static final List<String> PregnancyCodes = List.of("82810-3", "11636-8", "11637-6", "11638-4", "11639-2", "11640-0", "11612-9", "11613-7", "11614-5", "33065-4");

	public static Bundle buildFromSearch(IBundleProvider searchSet) {
		Bundle bundle = createIPSBundle();
		List<Resource> resourceList = createResourceList(searchSet.getAllResources());
		Composition composition = createIPSComposition(resourceList);
		bundle.addEntry().setResource(composition);
		for (Resource resource : resourceList) {
			bundle.addEntry().setResource(resource);
		}
		return bundle;
	}

	private static Bundle createIPSBundle() {
		Bundle bundle = new Bundle();
		bundle.setType(BundleType.DOCUMENT)
			.setTimestamp(new Date())
			.setId(IdDt.newRandomUuid());		
		return bundle;
	}

	private static List<Resource> createResourceList(List<IBaseResource> iBaseResourceList) {
		List<Resource> resourceList = new ArrayList<Resource>();
		for (IBaseResource ibaseResource : iBaseResourceList) {
			resourceList.add((Resource) ibaseResource);
		}
		return resourceList;
	}

	private static Composition createIPSComposition(List<Resource> resourceList) {
		Composition composition = new Composition();
		Patient patient = (Patient) resourceList.get(0);
		composition.setStatus(Composition.CompositionStatus.FINAL)
			.setType(new CodeableConcept().addCoding(new Coding().setCode("60591-5").setSystem("http://loinc.org").setDisplay("Patient Summary Document")))
			.setSubject(new Reference(patient))
			.setDate(new Date())
			.setTitle("Patient Summary as of " + DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDate.now()))
			.setConfidentiality(Composition.DocumentConfidentiality.N)
			// Should one of these be set to our system?
			// .setAuthor(List.of(new Reference(practitioner)))
			// .setCustodian(new Reference(organization))
			// .setRelatesTo(List.of(new Composition.RelatedComponent().setType(Composition.RelatedTypeEnum.SUBJECT).setTarget(new Reference(patient))))
			// .setEvent(List.of(new Composition.EventComponent().setCode(new CodeableConcept().addCoding(new Coding().setCode("PCPR").setSystem("http://terminology.hl7.org/CodeSystem/v3-ActClass").setDisplay("")))))
			.setId(IdDt.newRandomUuid());

		HashMap<IPSSection, List<Resource>> sortedResources = createIPSResourceHashMap(resourceList);
		for (IPSSection iPSSection : IPSSection.values()) {
			if (sortedResources.get(iPSSection) != null && sortedResources.get(iPSSection).size() > 0) {
				Composition.SectionComponent section = createSection(SectionText.get(iPSSection), sortedResources.get(iPSSection));
				composition.addSection(section);
			}
		}
		return composition;
	}

	private static HashMap<IPSSection, List<Resource>> createIPSResourceHashMap(List<Resource> resourceList) {
		HashMap<IPSSection, List<Resource>> iPSResourceMap = new HashMap<IPSSection, List<Resource>>();

		for (Resource resource : resourceList) {
			for (IPSSection iPSSection : IPSSection.values()) {
				if ( SectionTypes.get(iPSSection).contains(resource.getResourceType()) ) {
					if ( !(resource.getResourceType() == ResourceType.Observation) || isObservationinSection(iPSSection, (Observation) resource)) {
						if (iPSResourceMap.get(iPSSection) == null) {
							iPSResourceMap.put(iPSSection, new ArrayList<Resource>());
						}
						iPSResourceMap.get(iPSSection).add(resource);
					}
				}
			}
		}

		Patient patient = (Patient) resourceList.get(0);

		if (iPSResourceMap.get(IPSSection.ALLERGY_INTOLERANCE) == null) {
			AllergyIntolerance noInfoAllergies = noInfoAllergies(patient);
			resourceList.add(noInfoAllergies);
			iPSResourceMap.put(IPSSection.ALLERGY_INTOLERANCE, List.of(noInfoAllergies));
		}

		if (iPSResourceMap.get(IPSSection.MEDICATION_SUMMARY) == null) {
			MedicationStatement noInfoMedications = noInfoMedications(patient);
			resourceList.add(noInfoMedications);
			iPSResourceMap.put(IPSSection.MEDICATION_SUMMARY, List.of(noInfoMedications));
		}

		if (iPSResourceMap.get(IPSSection.PROBLEM_LIST) == null) {
			Condition noInfoProblems = noInfoProblems(patient);
			resourceList.add(noInfoProblems);
			iPSResourceMap.put(IPSSection.PROBLEM_LIST, List.of(noInfoProblems));
		}

		return iPSResourceMap;
	}

	private static AllergyIntolerance noInfoAllergies(Patient patient) {
		AllergyIntolerance allergy = new AllergyIntolerance();
		allergy.setCode(new CodeableConcept().addCoding(new Coding().setCode("no-allergy-info").setSystem("http://hl7.org/fhir/uv/ips/CodeSystem/absent-unknown-uv-ips").setDisplay("No information about allergies")))
			.setPatient(new Reference(patient))
			.setClinicalStatus(new CodeableConcept().addCoding(new Coding().setCode("active").setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")))
			.setId(IdDt.newRandomUuid());
		return allergy;
	}

	private static MedicationStatement noInfoMedications(Patient patient) {
		MedicationStatement medication = new MedicationStatement();
		// setMedicationCodeableConcept is not available
		medication.setCategory(new CodeableConcept().addCoding(new Coding().setCode("no-medication-info").setSystem("http://hl7.org/fhir/uv/ips/CodeSystem/absent-unknown-uv-ips").setDisplay("No information about medications")))
			.setSubject(new Reference(patient))
			.setStatus(MedicationStatement.MedicationStatementStatus.UNKNOWN)
			// .setEffective(new Period().addExtension().setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason").setValue((new Coding().setCode("not-applicable"))))
			.setId(IdDt.newRandomUuid());
		return medication;
	}

	private static Condition noInfoProblems(Patient patient) {
		Condition condition = new Condition();
		condition.setCode(new CodeableConcept().addCoding(new Coding().setCode("no-problem-info").setSystem("http://hl7.org/fhir/uv/ips/CodeSystem/absent-unknown-uv-ips").setDisplay("No information about problems")))
			.setSubject(new Reference(patient))
			.setClinicalStatus(new CodeableConcept().addCoding(new Coding().setCode("active").setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")))
			.setId(IdDt.newRandomUuid());
		return condition;
	}

	private static Boolean isObservationinSection(IPSSection iPSSection, Observation observation) {
		Boolean inSection = false;
		
		// We'll to check this logic again but at least the types match so it compiles
		switch(iPSSection) {
			case VITAL_SIGNS:
				if (observation.getCategory().get(0).getCoding().get(0).getCode() == "vital-signs") {
					inSection = true;
				}
				break;
			case PREGNANCY:
				if (PregnancyCodes.contains(observation.getCode().getCoding().get(0).getCode())) {
					inSection = true;
				}
				break;
			case SOCIAL_HISTORY:
				if (observation.getCategory().get(0).getCoding().get(0).getCode() == "social-history") {
					inSection = true;
				}
				break;
			case DIAGNOSTIC_RESULTS:
				if (observation.getCategory().get(0).getCoding().get(0).getCode() == "laboratory") {
					inSection = true;
				}
				break;
			}
		return inSection;
	}

	private static Composition.SectionComponent createSection(Map<String, String> text, List<Resource> resources) {
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
	
}
