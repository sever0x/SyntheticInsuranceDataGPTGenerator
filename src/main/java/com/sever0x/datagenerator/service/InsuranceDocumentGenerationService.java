package com.sever0x.datagenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sever0x.datagenerator.types.DocumentType;
import com.sever0x.datagenerator.types.InsuranceCompanyType;
import com.sever0x.datagenerator.data.InsuranceEntities;
import com.sever0x.datagenerator.types.WritingStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class InsuranceDocumentGenerationService {
	private final OpenAiChatModel openAiChatModel;
	private final ObjectMapper objectMapper;

	public InsuranceDocumentGenerationService(
			OpenAiChatModel openAiChatModel,
			ObjectMapper objectMapper
	) {
		this.openAiChatModel = openAiChatModel;
		this.objectMapper = objectMapper;
	}

	private static final String SYSTEM_PROMPT = """
        Du bist ein Experte für deutsche Versicherungsdokumente mit 15 Jahren Erfahrung in der Branche.
        
        AUFGABE: Generiere realistische deutsche Versicherungsdokumente mit korrekter Terminologie.
        
        WICHTIGE ANFORDERUNGEN:
        1. Verwende authentische deutsche Versicherungssprache und -begriffe
        2. Inkludiere immer diese PFLICHT-ENTITÄTEN:
           - Vertragsnummer (verschiedene Formate: VS-YYYY-XXXXXX, POL-XXXXXXXX, etc.)
           - Kundennummer (KD-XXXXXX, KUNDE-XXXXXXXX, etc.)
           - Firmenname (echte deutsche Versicherungsunternehmen)
           - Personennamen (realistische deutsche Namen mit Titeln)
        
        3. SCHREIBSTILE variieren:
           - Formell (große Versicherungen)
           - Persönlich (kleinere Makler)
           - Technisch (Schadensgutachten)
           - Freundlich (Kundenservice)
           - Geschäftlich (B2B Kommunikation)
        
        4. Verwende verschiedene Briefköpfe, Layouts und Formulierungen
        5. Dokumente sollen von verschiedenen "Personen" und Abteilungen stammen
        6. Inkludiere branchenspezifische Details (Policennummern, Deckungssummen, etc.)
        
        KONTEXT: Diese Dokumente werden für NER-Training verwendet - Entitäten müssen klar erkennbar sein.
        """;

	public String generatePolicyConfirmation() {
		String userPrompt = """
            Generiere eine VERSICHERUNGSPOLICE-BESTÄTIGUNG auf Deutsch.
           \s
            SPEZIFIKATIONEN:
            - Typ: Vertragsbestätigung für [zufällige Versicherungsart]
            - Schreibstil: [wähle: formell/freundlich/geschäftlich]
            - Absender: [deutsche Versicherungsgesellschaft]
            - Länge: 150-250 Wörter
           \s
            MUSS ENTHALTEN:
            ✓ Briefkopf mit Firmenadresse
            ✓ Anrede (Sehr geehrte/r...)
            ✓ Vertragsnummer (Format: VS-YYYY-XXXXXX oder ähnlich)
            ✓ Kundennummer (Format: KD-XXXXXX oder ähnlich) \s
            ✓ Versicherungsgesellschaft-Name
            ✓ Ansprechpartner-Name
            ✓ Versicherungssumme und Beitrag
            ✓ Unterschrift/Grußformel
           \s
            VERSICHERUNGSARTEN (wähle eine):
            - Kfz-Versicherung, Hausratversicherung, Haftpflichtversicherung
            - Lebensversicherung, Berufsunfähigkeitsversicherung
            - Rechtsschutzversicherung, Reiseversicherung
           \s
            Erstelle ein authentisches deutsches Dokument!
           \s""";

		return callOpenAI(userPrompt);
	}

	public String generateClaimReport() {
		String userPrompt = """
            Generiere eine SCHADENSMELDUNG auf Deutsch.
            
            SPEZIFIKATIONEN:
            - Typ: Schadensmeldung/Schadensanzeige
            - Schreibstil: [wähle: technisch/formal/detailliert]
            - Context: Kunde meldet Schaden bei Versicherung
            - Länge: 200-300 Wörter
            
            MUSS ENTHALTEN:
            ✓ Schadensmeldung Überschrift
            ✓ Versicherungsnehmer-Daten mit Name
            ✓ Vertragsnummer und Police-Nummer
            ✓ Kundennummer
            ✓ Versicherungsgesellschaft
            ✓ Schadenstag und -zeit
            ✓ Schadensnummer (Format: S-XXXXXXXXXX)
            ✓ Schadenschilderung mit EUR-Betrag
            ✓ Beteiligte Personen/Unternehmen
            
            SCHADENARTEN (wähle eine):
            - Verkehrsunfall, Wasserschaden, Einbruch
            - Sturm-/Hagelschaden, Glasbruch, Diebstahl
            - Berufshaftpflicht-Fall, Rechtsstreit
            
            Verwende authentische Versicherungsterminologie!
            """;

		return callOpenAI(userPrompt);
	}

	public String generatePremiumAdjustment() {
		String userPrompt = """
            Generiere eine BEITRAGSANPASSUNG-MITTEILUNG auf Deutsch.
           \s
            SPEZIFIKATIONEN:
            - Typ: Information über Beitragserhöhung/-senkung
            - Schreibstil: [wähle: geschäftlich/erklärend/entschuldigend]
            - Absender: Versicherungsunternehmen
            - Länge: 150-220 Wörter
           \s
            MUSS ENTHALTEN:
            ✓ Firmenbriefkopf
            ✓ Betreff: "Beitragsanpassung zum [Datum]"
            ✓ Kundendaten (Name, Anschrift)
            ✓ Vertragsnummer
            ✓ Kundennummer \s
            ✓ Versicherungsart
            ✓ Alter vs. neuer Jahresbeitrag (EUR-Beträge)
            ✓ Begründung der Anpassung
            ✓ Ansprechpartner mit Telefonnummer
            ✓ Grußformel
           \s
            ANPASSUNGSGRÜNDE (wähle einen):
            - Allgemeine Kostenentwicklung
            - Veränderte Schadensituation \s
            - Neue Deckungsbausteine
            - Versicherungssteuer-Änderung
            - Inflationsausgleich
           \s
            Sei höflich aber sachlich!
           \s""";

		return callOpenAI(userPrompt);
	}

	public String generateCancellationLetter() {
		String userPrompt = """
            Generiere ein KÜNDIGUNGS-SCHREIBEN auf Deutsch.
            
            SPEZIFIKATIONEN:
            - Typ: [wähle: Kündigung durch Kunde ODER durch Versicherung]
            - Schreibstil: [wähle: formal/rechtlich/freundlich-bestimmt]
            - Perspektive: [Kunde kündigt] oder [Versicherung kündigt]
            - Länge: 120-200 Wörter
            
            MUSS ENTHALTEN:
            ✓ Briefkopf (Absender/Empfänger)
            ✓ Betreff: "Kündigung Versicherungsvertrag"
            ✓ Anrede
            ✓ Kündigungsformulierung mit Fristen
            ✓ Vertragsnummer
            ✓ Kundennummer
            ✓ Versicherungsart und Versicherungsgesellschaft
            ✓ Kündigungsgrund (optional)
            ✓ Bitte um Bestätigung
            ✓ Unterschrift/Grußformel
            
            KÜNDIGUNGSARTEN:
            - Ordentliche Kündigung zum Vertragsende
            - Außerordentliche Kündigung (Schadenfall)
            - Kündigung nach Beitragserhöhung
            - Kündigung wegen Umzug/Lebenswandel
            
            Verwende korrekte Rechtsterminologie!
            """;

		return callOpenAI(userPrompt);
	}

	public String generatePaymentReminder() {
		String userPrompt = """
            Generiere eine ZAHLUNGSERINNERUNG/MAHNUNG auf Deutsch.
            
            SPEZIFIKATIONEN:
            - Typ: [wähle: 1. Erinnerung, 2. Mahnung, oder 3. Mahnung]
            - Schreibstil: [wähle: freundlich-mahnend/bestimmt/rechtlich-scharf]
            - Absender: Versicherungsunternehmen/Inkasso
            - Länge: 130-200 Wörter
            
            MUSS ENTHALTEN:
            ✓ Firmenbriefkopf mit Mahnabteilung
            ✓ Betreff mit "Zahlungserinnerung" oder "Mahnung"
            ✓ Kundendaten
            ✓ Vertragsnummer
            ✓ Kundennummer
            ✓ Fälliger Betrag (EUR)
            ✓ Fälligkeitsdatum (überschritten)
            ✓ Zahlungsaufforderung mit neuer Frist
            ✓ Konsequenzen bei Nichtzahlung
            ✓ Bankverbindung (IBAN)
            ✓ Sachbearbeiter-Name
            
            MAHNUNGSGRADE:
            - 1. Freundliche Erinnerung (Versehen?)
            - 2. Bestimmte Mahnung (Mahngebühren)
            - 3. Letzte Mahnung (Anwalt/Inkasso-Drohung)
            
            Ton sollte angemessen eskalieren!
            """;

		return callOpenAI(userPrompt);
	}

	public String generateInsuranceQuote() {
		String userPrompt = """
            Generiere ein VERSICHERUNGS-ANGEBOT auf Deutsch.
            
            SPEZIFIKATIONEN:
            - Typ: Angebot für Neukundenakquise
            - Schreibstil: [wähle: verkaufsorientiert/beratend/kompetent]
            - Absender: [Versicherungsmakler ODER Direktversicherer]
            - Länge: 200-280 Wörter
            
            MUSS ENTHALTEN:
            ✓ Makler-/Versicherungs-Briefkopf
            ✓ Anrede (persönlich)
            ✓ Angebotsnummer (ANG-XXXXXXXX)
            ✓ Kundennummer (falls Bestandskunde)
            ✓ Angebotene Versicherungsart
            ✓ Versicherungsgesellschaft
            ✓ Deckungsumfang/Versicherungssumme
            ✓ Jahresbeitrag und Zahlungsweise
            ✓ Laufzeit und Kündigungsfristen
            ✓ Ansprechpartner-Name
            ✓ Gültigkeitsdauer des Angebots
            
            ANGEBOTS-SZENARIEN:
            - Familien-Absicherung (Leben, BU, Haftpflicht)
            - Gewerbeversicherung (Betriebshaftpflicht, Cyber)
            - Immobilien-Schutz (Gebäude, Hausrat)
            - Kfz-Vollkasko mit Zusatzbausteinen
            
            Verkaufe professionell aber nicht aufdringlich!
            """;

		return callOpenAI(userPrompt);
	}

	public InsuranceEntities extractEntities(String documentText) {
		String extractionPrompt = """
            Analysiere das folgende deutsche Versicherungsdokument und extrahiere alle Entitäten.
           \s
            DOKUMENT:
            %s
           \s
            AUFGABE: Finde und kategorisiere alle relevanten Entitäten. Gib das Ergebnis als JSON zurück.
           \s
            JSON FORMAT:
            {
              "contract_numbers": ["VS-2024-123456", "POL-987654321"],
              "customer_ids": ["KD-123456", "KUNDE-12345678"],
              "company_names": ["Allianz Versicherungs-AG", "AXA Deutschland"],
              "person_names": ["Herr Andreas Müller", "Dr. Petra Schmidt"],
              "amounts": ["EUR 50.000,00", "EUR 1.250,00"],
              "dates": ["15.03.2024", "01.04.2024"],
              "addresses": ["Hauptstraße 15, 10115 Berlin"]
            }
           \s
            ERKENNUNGSREGELN:
            - Contract Numbers: VS-, POL-, KV-, VN, LV-, HV-, RV-, UV- + Nummern
            - Customer IDs: KD-, KUNDE-, KN, M- + Nummern \s
            - Company Names: Versicherungsgesellschaften, Makler, mit AG/GmbH
            - Person Names: Herr/Frau/Dr./Prof. + Vor- und Nachname
            - Amounts: EUR-Beträge, Versicherungssummen, Beiträge
            - Dates: Datumsangaben (TT.MM.JJJJ)
            - Addresses: Vollständige Adressen mit Straße, PLZ, Ort
           \s
            Gib NUR das JSON zurück, keine zusätzlichen Erklärungen!
           \s""".formatted(documentText);

		String response = callOpenAI(extractionPrompt);

		try {
			return objectMapper.readValue(response, InsuranceEntities.class);
		} catch (Exception e) {
			log.error("Failed to parse entity extraction response", e);
			return new InsuranceEntities();
		}
	}

	public String generateWithPersonality(DocumentType docType, WritingStyle style, InsuranceCompanyType companyType) {
		String personalityPrompt = """
            Generiere ein %s auf Deutsch mit folgenden Charakteristika:
           \s
            PERSÖNLICHKEIT & STIL:
            - Schreibstil: %s
            - Unternehmenstyp: %s
            - Autor-Persönlichkeit: %s
           \s
            VARIATION REQUIREMENTS:
            - Verwende unterschiedliche Formulierungen und Ausdrücke
            - Variiere Briefköpfe und Layout-Struktur \s
            - Verschiedene Anrede-/Grußformeln
            - Realistische Unterschiede in Formalität
            - Andere Nummernsysteme und Referenzen
           \s
            %s
           \s""".formatted(
				docType.getGermanName(),
				style.getDescription(),
				companyType.getDescription(),
				generateRandomPersonality(),
				getDocumentSpecificRequirements(docType)
		);

		return callOpenAI(personalityPrompt);
	}

	private String getDocumentSpecificRequirements(DocumentType docType) {
		return switch (docType) {
			case POLICY_CONFIRMATION -> """
                SPEZIFISCHE ANFORDERUNGEN:
                ✓ Vollständige Vertragsdaten (Nummer, Laufzeit, Beitrag)
                ✓ Deckungsumfang und Versicherungssumme \s
                ✓ Zahlungsmodalitäten
                ✓ Kontaktdaten für Rückfragen
                ✓ Rechtliche Hinweise und Widerrufsbelehrung
               \s""";

			case CLAIM_REPORT -> """
                SPEZIFISCHE ANFORDERUNGEN:
                ✓ Detaillierte Schadensschilderung
                ✓ Schadenshöhe in EUR
                ✓ Beteiligte Personen/Fahrzeuge
                ✓ Polizeiaktenzeichen (wenn vorhanden)
                ✓ Zeugenangaben
                ✓ Gutachter-/Werkstatttermine
                """;

			case PREMIUM_ADJUSTMENT -> """
                SPEZIFISCHE ANFORDERUNGEN:
                ✓ Klare Gegenüberstellung: Alt vs. Neu
                ✓ Prozentuale Änderung des Beitrags
                ✓ Begründung der Anpassung
                ✓ Datum des Inkrafttretens
                ✓ Sonderkündigungsrecht-Hinweis
                """;

			case CANCELLATION -> """
                SPEZIFISCHE ANFORDERUNGEN:
                ✓ Eindeutige Kündigungserklärung
                ✓ Kündigungsfristen beachten
                ✓ Grund der Kündigung (optional)
                ✓ Datum der gewünschten Beendigung
                ✓ Bitte um schriftliche Bestätigung
                ✓ Regelung für Restbeiträge
                """;

			case PAYMENT_REMINDER -> """
                SPEZIFISCHE ANFORDERUNGEN:
                ✓ Offener Betrag und Fälligkeitsdatum
                ✓ Neue Zahlungsfrist (meist 14 Tage)
                ✓ Konsequenzen bei Nichtzahlung
                ✓ Bankverbindung für Überweisung
                ✓ Mahngebühren (bei 2./3. Mahnung)
                ✓ Ansprechpartner für Rückfragen
                """;

			case INSURANCE_QUOTE -> """
                SPEZIFISCHE ANFORDERUNGEN:
                ✓ Attraktive Präsentation der Vorteile
                ✓ Vergleich zu Mitbewerbern (optional)
                ✓ Flexible Zahlungsoptionen
                ✓ Zusatzbausteine und Upgrades
                ✓ Gültigkeitsdauer des Angebots
                ✓ Call-to-Action für Vertragsabschluss
                """;
		};
	}

	private String generateRandomPersonality() {
		String[] personalities = {
				"Erfahrener Sachbearbeiter (detailorientiert, gründlich)",
				"Freundlicher Kundenberater (persönlich, lösungsorientiert)",
				"Strenger Jurist (präzise, formal, rechtlich korrekt)",
				"Empathische Schadensreguliererin (verständnisvoll, professionell)",
				"Effizienter Makler (verkaufsorientiert, zeitbewusst)",
				"Geduldiger Trainer (erklärend, strukturiert)",
				"Pragmatischer Teamleiter (direkt, ergebnisorientiert)"
		};
		return personalities[new Random().nextInt(personalities.length)];
	}

	public String generateComplexDocument() {
		String complexPrompt = """
            Generiere ein KOMPLEXES Versicherungsdokument mit MEHREREN Verträgen und Beteiligten.
           \s
            SZENARIO: Firmenversicherung mit mehreren Policen
            - Hauptvertrag + 2-3 Zusatzverträge
            - Verschiedene Kundennummern für Tochtergesellschaften
            - Mehrere Ansprechpartner
            - Cross-references zwischen Verträgen
           \s
            MUSS ENTHALTEN:
            ✓ 3-4 verschiedene Vertragsnummern
            ✓ 2-3 verschiedene Kundennummern \s
            ✓ Haupt- und Tochtergesellschaften
            ✓ Mehrere Sachbearbeiter-Namen
            ✓ Verweise zwischen den Verträgen
            ✓ Verschiedene Versicherungsarten
           \s
            Erstelle ein realistisches B2B-Szenario!
           \s""";
		return callOpenAI(complexPrompt);
	}

	public String generateEdgeCaseDocument() {
		String edgeCasePrompt = """
            Generiere ein Versicherungsdokument mit SCHWIERIGEN Entity-Erkennungsmustern.
            
            HERAUSFORDERUNGEN:
            - Vertragsnummern in Fließtext eingebettet
            - Ähnliche Nummern die KEINE Entities sind (Telefon, Datum)
            - Abgekürzte/verkürzte Firmennamen
            - Informelle Personennamen ohne Titel
            - Mehrdeutige Referenzen
            
            BEISPIEL-SCHWIERIGKEITEN:
            "Bezugnehmend auf Ihr Schreiben vom 15.03.2024 (AZ: ABC-123) bezüglich Vertrag VS-2024-987654..."
            "Die Telefonnummer 0123-456789 ist nicht zu verwechseln mit der Policennummer POL-456789..."
            
            Erstelle ein Document das NER-Modelle herausfordert!
            """;
		return callOpenAI(edgeCasePrompt);
	}

	public String generateMultiLanguageDocument() {
		String multiLangPrompt = """
            Generiere ein deutsches Versicherungsdokument mit INTERNATIONALEN Elementen.
            
            SZENARIO: Internationale Versicherung oder Auslandsschutz
            - Hauptsprache: Deutsch
            - Einzelne englische/französische Begriffe (authentisch)
            - Internationale Firmennamen
            - Ausländische Adressen/Referenzen
            
            BEISPIELE:
            - "Global Liability Coverage" als Produktname
            - Französische Rückversicherung: "AXA Réassurance"
            - Englische Fachbegriffe: "Claims Management", "Underwriting"
            
            Bleibe authentisch deutsch mit realistischen internationalen Touches!
            """;
		return callOpenAI(multiLangPrompt);
	}

	private String callOpenAI(String userPrompt) {
		try {
			ChatResponse response = openAiChatModel.call(
					new Prompt(List.of(
							new SystemMessage(SYSTEM_PROMPT),
							new UserMessage(userPrompt)
					),
							OpenAiChatOptions.builder()
									.model(OpenAiApi.ChatModel.GPT_4_1_NANO)
									.temperature(0.8)
									.maxTokens(800)
									.build())
			);

			return response.getResult().getOutput().getText();

		} catch (Exception e) {
			log.error("OpenAI API call failed", e);
			throw new RuntimeException("Failed to generate document", e);
		}
	}
}
