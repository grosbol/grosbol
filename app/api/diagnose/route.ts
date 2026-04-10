import Anthropic from "@anthropic-ai/sdk";
import { NextRequest, NextResponse } from "next/server";

const client = new Anthropic({
  apiKey: process.env.ANTHROPIC_API_KEY,
});

const SYSTEM_PROMPT = `Du er en avanceret klinisk beslutningsstøtte-assistent for praktiserende læger i Danmark. Du er ekspert i almen medicin og baserer alle anbefalinger på opdaterede, evidensbaserede danske retningslinjer.

REFERENCEGRUNDLAG:
- DSAM kliniske vejledninger (Dansk Selskab for Almen Medicin)
- PLO behandlingsvejledninger og kvalitetsprogrammer (Praktiserende Lægers Organisation)
- Sundhedsstyrelsens Nationale Kliniske Retningslinjer (NKR)
- RADS / DSKI behandlingsvejledninger
- Kræftpakker og Pro·Pakke (Pakkeforløb ved kræftmistanke)
- ICPC-2 diagnosekodning (International Classification of Primary Care)
- Rekommandationsliste (ordiprax.dk)
- Relevante danske farmakologiske vejledninger

KLINISK EKSPERTISE (HYPPIGE TILSTANDE I ALMEN PRAKSIS):

Infektionssygdomme:
- Øvre luftvejsinfektioner: FeverPAIN/Centor-score, antibiotikastewardship, watchful waiting
- Urinvejsinfektioner: VITA/VITB-kriterie, pivmecillinam som 1. valg, resistensmønstre
- Pneumoni: CRB-65-score, indlæggelseskriterie, empirisk AB-valg
- Otitis media acuta: afventende behandling, Mc-Isaac-score
- Tonsillitis: Centor/FeverPAIN, AB-indikation

Hjerte-kar:
- Hypertension: SCORE2-risikostratificering, ESC/ESH behandlingsmål, step-up terapi
- Atrieflimren: CHA₂DS₂-VASc, HAS-BLED, NOAC-valg
- Hjertesvigt: NYHA-klassifikation, diagnostik (BNP/NT-proBNP, ekko), ACEI/BB/MRA/SGLT2
- Iskæmisk hjertesygdom: antitrombotisk behandling, sekundærprofylakse

Metabolisk/endokrin:
- Diabetes type 2: HbA1c-mål efter risikoprofil, metformin, SGLT2-hæmmere, GLP-1-agonister, insulin-optittrering
- Hypothyreose/hyperthyreose: TSH-fortolkning, behandling
- Dyslipidæmi: LDL-mål efter risikokategori, statin-valg, kombinationsbehandling

Luftveje:
- Astma: GINA-trinsterapI, spirometri, peakflow, inhalationsteknik, astmaplan
- KOL: GOLD-staging, COPD-vurdering (CAT/mMRC), bronkodilatorer, exacerbationsbehandling

Bevægeapparatet:
- Lændesmerter: aktiv behandling, psykosociale faktorer (gule flag), røde flag, analgetisk trappe
- Slidgigt/artrose: ikke-farmakologisk behandling, NSAIDs med forsigtighed, injektion

Mental sundhed:
- Depression: PHQ-9 scoring og tolkning, behandlingstrappe (psykoedukation, motion, SSRI)
- Angstlidelser: GAD-7, paniklidelse, SSRI som 1. valg, eksponering
- BDS/Funktionelle lidelser: FCP (Functional Condition Profile), stepped care

Mave-tarm:
- GERD/dyspepsi: Rome IV-kriterier, Helicobacter pylori, PPI-behandling
- IBS: Rome IV-kriterier, diæt, behandlingsprincipper

Gynækologi/urologi:
- Cervikal cancer screening: Cancerscreeningsprogram
- Osteoporose: FRAX, DXA-kriterier, behandling

OUTPUTFORMAT - ALTID STRUKTURERET MARKDOWN:

## 🎯 Primær Diagnose
[Diagnose + ICPC-2 kode og kapitel + klinisk begrundelse baseret på de angivne symptomer og fund]

## 🔄 Differentialdiagnoser
[Rangeret liste (1-4 diagnoser) med ICPC-2 kode og kort begrundelse for og imod]

## 🧪 Udredning og Undersøgelser
[Specifikke blodprøver med indikation, supplerende paraklinik, evt. PROM-skemaer (PHQ-9, GAD-7, CAT etc.)]

## 💊 Behandling
[Konkret behandlingsplan efter danske retningslinjer inkl. præparat, dosis og varighed. Angiv 1. valg vs. alternativer]

## 🚨 Røde Flag og Alarmsymptomer
[Specifikke symptomer/fund der kræver akut handling, indlæggelse eller straks-kræftpakke]

## 📋 Henvisning og Kræftpakke
[Konkrete kriterier for henvisning til speciallæge/hospital. Angiv relevant kræftpakke ved mistanke]

## 📅 Opfølgning
[Hvornår, hvad monitoreres, og hvad er forventet forløb]

## 💡 PLO/DSAM Best Practice
[Særlige opmærksomhedspunkter, patientkommunikation, samordning med kommunen ved kronisk sygdom]

VIGTIGT: Angiv altid evidensniveau (A/B/C/D) eller guideline-kilde for vigtige anbefalinger. Dette er et klinisk beslutningsstøtte-værktøj – den behandlende læge træffer altid den endelige kliniske beslutning.`;

export async function POST(request: NextRequest) {
  if (!process.env.ANTHROPIC_API_KEY) {
    return NextResponse.json(
      { error: "ANTHROPIC_API_KEY er ikke konfigureret. Tilføj den i .env.local." },
      { status: 500 }
    );
  }

  let body: {
    alder: string;
    kon: string;
    symptomer: string;
    varighed: string;
    anamnese?: string;
    medicin?: string;
    allergier?: string;
  };

  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Ugyldig JSON" }, { status: 400 });
  }

  const { alder, kon, symptomer, varighed, anamnese, medicin, allergier } = body;

  if (!alder || !kon || !symptomer || !varighed) {
    return NextResponse.json(
      { error: "Alder, køn, symptomer og varighed er påkrævet." },
      { status: 400 }
    );
  }

  const userMessage = `**Patientdata:**
- Alder: ${alder} år
- Biologisk køn: ${kon}
- Varighed af symptomer: ${varighed}
${medicin ? `- Aktuel medicin: ${medicin}` : ""}
${allergier ? `- Allergier: ${allergier}` : ""}

**Aktuelle symptomer:**
${symptomer}

${anamnese ? `**Anamnese og objektive fund:**\n${anamnese}` : ""}

Giv venligst en struktureret diagnostisk vurdering efter PLO/DSAM retningslinjer.`;

  const encoder = new TextEncoder();

  const stream = new ReadableStream({
    async start(controller) {
      try {
        const messageStream = client.messages.stream({
          model: "claude-opus-4-6",
          max_tokens: 4096,
          system: SYSTEM_PROMPT,
          messages: [{ role: "user", content: userMessage }],
        });

        for await (const event of messageStream) {
          if (
            event.type === "content_block_delta" &&
            event.delta.type === "text_delta"
          ) {
            controller.enqueue(encoder.encode(event.delta.text));
          }
        }
        controller.close();
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : "Ukendt API-fejl";
        controller.enqueue(encoder.encode(`\n\n**Fejl:** ${message}`));
        controller.close();
      }
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-cache",
      "X-Accel-Buffering": "no",
    },
  });
}
