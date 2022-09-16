package ca.uhn.fhir.jpa.provider.r4.patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
// import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.instance.model.api.IBaseResource;

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

	public static Bundle buildFromSearch(IBundleProvider searchSet, FhirContext ctx) {	
		List<Resource> searchResources = createResourceList(searchSet.getAllResources());
		Patient patient = (Patient) searchResources.get(0);
		HashMap<IPSSection, List<Resource>> initialHashedPrimaries = hashPrimaries(searchResources);
		List<Resource> expandedResources = addNoInfoResources(searchResources, initialHashedPrimaries, patient);
		HashMap<IPSSection, List<Resource>> hashedPrimaries = hashPrimaries(expandedResources);
		HashMap<IPSSection, List<Resource>> filteredPrimaries = filterPrimaries(hashedPrimaries);
		List<Resource> resources = pruneResources(expandedResources, filteredPrimaries);
		HashMap<IPSSection, String> hashedNarratives = createNarratives(filteredPrimaries, resources, ctx);

		Bundle bundle = createIPSBundle();
		Composition composition = createIPSComposition(patient);
		composition = addIPSSections(composition, hashedPrimaries, hashedNarratives);
		bundle.addEntry().setResource(composition).setFullUrl(composition.getIdElement().getValue());
		for (Resource resource : resources) {
			bundle.addEntry().setResource(resource).setFullUrl(resource.getIdElement().getValue());
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

	private static HashMap<IPSSection, List<Resource>> hashPrimaries(List<Resource> resourceList) {
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

		return iPSResourceMap;
	}

	private static List<Resource> addNoInfoResources(List<Resource> resources,  HashMap<IPSSection, List<Resource>> hashedPrimaries, Patient patient) {

		if (hashedPrimaries.get(IPSSection.ALLERGY_INTOLERANCE) == null) {
			AllergyIntolerance noInfoAllergies = noInfoAllergies(patient);
			resources.add(noInfoAllergies);
		}

		if (hashedPrimaries.get(IPSSection.MEDICATION_SUMMARY) == null) {
			MedicationStatement noInfoMedications = noInfoMedications(patient);
			resources.add(noInfoMedications);
		}

		if (hashedPrimaries.get(IPSSection.PROBLEM_LIST) == null) {
			Condition noInfoProblems = noInfoProblems(patient);
			resources.add(noInfoProblems);
		}

		return resources;
	}

	private static HashMap<IPSSection, List<Resource>> filterPrimaries(HashMap<IPSSection, List<Resource>> hashedPrimaries) {
		HashMap<IPSSection, List<Resource>> filteredPrimaries = new HashMap<IPSSection, List<Resource>>();
		for ( IPSSection section : hashedPrimaries.keySet() ) {
			List<Resource> filteredList = new ArrayList<Resource>();
			for (Resource resource : hashedPrimaries.get(section)) {
				if (passesFilter(section, resource)) {
					filteredList.add(resource);
				}
			}
			if (filteredList.size() > 0) {
				filteredPrimaries.put(section, filteredList);
			}
		}
		return filteredPrimaries;
	}

	private static List<Resource> pruneResources(List<Resource> resources,  HashMap<IPSSection, List<Resource>> hashedPrimaries) {
		// Stubbed out for now
		// hashedPrimaries.values().stream().flatMap(List::stream).collect(Collectors.toList());

		return resources;
	}

	private static HashMap<IPSSection, String> createNarratives(HashMap<IPSSection, List<Resource>> hashedPrimaries, List<Resource> resources, FhirContext ctx) {
		HashMap<IPSSection, String> hashedNarratives = new HashMap<IPSSection, String>();

		for (IPSSection section : hashedPrimaries.keySet()) {
			// This method msy need to also take in the resources list for things such as medications and devices.
			String narrative = createSectionNarrative(section, hashedPrimaries.get(section), ctx);
			hashedNarratives.put(section, narrative);
		}

		return hashedNarratives;
	}

	private static String createSectionNarrative(IPSSection iPSSection, List<Resource> resources, FhirContext ctx) {
		// // Use the narrative generator
		// ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		// // Create a bundle to hold the resources
		// Bundle bundle = new Bundle();
		// Composition composition = new Composition();

		// bundle.addEntry().setResource(composition);
		// for (Resource resource : resources) {
		// 	bundle.addEntry().setResource(resource);
		// }

		// Need to look up profile for each section
		// String profile = "http://hl7.org/fhir/uv/ips/StructureDefinition/AllergyIntolerance-uv-ips"
		// bundle.setMeta(new Meta().addProfile(profile));

		// // Generate the narrative
		// DefaultThymeleafNarrativeGenerator generator = new DefaultThymeleafNarrativeGenerator();
		// generator.populateResourceNarrative(ctx, bundle);
		
		// // Get the narrative
		// String narrative = composition.getText().getDivAsString();
		
		// Stubbed out for now
		String narrative = "<div>Narrative</div>";
		
		return narrative;
	}

	private static Boolean passesFilter(IPSSection section, Resource resource) {
		if (section == IPSSection.ALLERGY_INTOLERANCE) {
			return true;
		}
		if (section == IPSSection.MEDICATION_SUMMARY) {
			return true;
		}
		if (section == IPSSection.PROBLEM_LIST) {
			return true;
		}
		if (section == IPSSection.IMMUNIZATIONS) {
			return true;
		}
		if (section == IPSSection.PROCEDURES) {
			return true;
		}
		if (section == IPSSection.MEDICAL_DEVICES) {
			return true;
		}
		if (section == IPSSection.DIAGNOSTIC_RESULTS) {
			return true;
		}
		if (section == IPSSection.VITAL_SIGNS) {
			return true;
		}
		if (section == IPSSection.PREGNANCY) {
			Observation observation = (Observation) resource;
			return (observation.getStatus() == ObservationStatus.PRELIMINARY);
		}
		if (section == IPSSection.SOCIAL_HISTORY) {
			Observation observation = (Observation) resource;
			return (observation.getStatus() == ObservationStatus.PRELIMINARY);
		}
		// if (section == IPSSection.ILLNESS_HISTORY) {
		// 	return true;
		// }
		if (section == IPSSection.FUNCTIONAL_STATUS) {
			return true;
		}
		if (section == IPSSection.PLAN_OF_CARE) {
			return true;
		}
		if (section == IPSSection.ADVANCE_DIRECTIVES) {
			return true;
		}
		return false;
	}

	private static Composition createIPSComposition(Patient patient) {
		Composition composition = new Composition();
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
		return composition;
	}

	private static Composition addIPSSections(Composition composition, HashMap<IPSSection, List<Resource>> hashedPrimaries, HashMap<IPSSection, String> hashedNarratives) {
		// Add sections
		for (IPSSection iPSSection : IPSSection.values()) {
			if (hashedPrimaries.get(iPSSection) != null && hashedPrimaries.get(iPSSection).size() > 0) {
				Composition.SectionComponent section = createSection(SectionText.get(iPSSection), hashedPrimaries.get(iPSSection), hashedNarratives.get(iPSSection));
				composition.addSection(section);
			}
		}
		return composition;
	}

	private static Composition.SectionComponent createSection(Map<String, String> text, List<Resource> resources, String narrative) {
		Composition.SectionComponent section = new Composition.SectionComponent();
		
		section.setTitle(text.get("title"))
			.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org")
			.setCode(text.get("code")).setDisplay(text.get("display"))))
			.getText().setStatus(Narrative.NarrativeStatus.GENERATED).setDivAsString(narrative);
		
		HashMap<ResourceType, List<Resource>> resourcesByType = new HashMap<ResourceType, List<Resource>>();
		
		for (Resource resource : resources) {
			if ( !resourcesByType.containsKey(resource.getResourceType()) ) {
				resourcesByType.put(resource.getResourceType(), new ArrayList<Resource>());
			}
			resourcesByType.get(resource.getResourceType()).add(resource);
		}
		
		for (List<Resource> resourceList : resourcesByType.values()) {	
			for (Resource resource : resourceList) {
				section.addEntry(new Reference(resource));
			}
		}

		return section;
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
		
		switch(iPSSection) {
			case VITAL_SIGNS:
				if (observation.hasCategory() && hasSpecficCode(observation.getCategory(), "vital-signs")) {
					inSection = true;
				}
				break;
			case PREGNANCY:
				if (observation.hasCode() && hasPregnancyCode(observation.getCode())) {
					inSection = true;
				}
				break;
			case SOCIAL_HISTORY:
				if (observation.hasCategory() && hasSpecficCode(observation.getCategory(), "social-history")) {
					inSection = true;
				}
				break;
			case DIAGNOSTIC_RESULTS:
				if (observation.hasCategory() && hasSpecficCode(observation.getCategory(), "laboratory")) {
					inSection = true;
				}
				break;
			}
		return inSection;
	}

	private static boolean hasPregnancyCode(CodeableConcept concept) {
		for (Coding c : concept.getCoding()) {
			if (PregnancyCodes.contains(c.getCode()))
				return true;
		}
	   return false;
	}

	private static boolean hasSpecficCode(List<CodeableConcept> ccList, String code) {
	   for (CodeableConcept concept : ccList) {
			for (Coding c : concept.getCoding()) {
				if (code.equals(c.getCode()))
					return true;
			}
		}
	   return false;
	}
	
}
