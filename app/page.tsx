"use client";

import { useState, useRef } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface PatientData {
  alder: string;
  kon: string;
  symptomer: string;
  varighed: string;
  anamnese: string;
  medicin: string;
  allergier: string;
}

const initialState: PatientData = {
  alder: "",
  kon: "",
  symptomer: "",
  varighed: "",
  anamnese: "",
  medicin: "",
  allergier: "",
};

const varighedOptions = [
  "< 24 timer",
  "1-3 dage",
  "4-7 dage",
  "1-2 uger",
  "2-4 uger",
  "1-3 måneder",
  "3-6 måneder",
  "> 6 måneder",
  "Kronisk / tilbagevendende",
];

export default function Home() {
  const [form, setForm] = useState<PatientData>(initialState);
  const [result, setResult] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.alder || !form.kon || !form.symptomer || !form.varighed) {
      setError("Udfyld venligst: alder, køn, symptomer og varighed.");
      return;
    }

    setResult("");
    setError("");
    setDone(false);
    setLoading(true);

    abortRef.current = new AbortController();

    try {
      const res = await fetch("/api/diagnose", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
        signal: abortRef.current.signal,
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: "Ukendt fejl" }));
        throw new Error(err.error || `HTTP ${res.status}`);
      }

      const reader = res.body!.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { done: streamDone, value } = await reader.read();
        if (streamDone) break;
        setResult((prev) => prev + decoder.decode(value, { stream: true }));
      }
      setDone(true);
    } catch (err: unknown) {
      if (err instanceof Error && err.name !== "AbortError") {
        setError(err.message || "Der opstod en fejl. Prøv igen.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleStop = () => {
    abortRef.current?.abort();
    setLoading(false);
    setDone(true);
  };

  const handleReset = () => {
    abortRef.current?.abort();
    setForm(initialState);
    setResult("");
    setError("");
    setDone(false);
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-700 rounded-lg flex items-center justify-center">
              <svg
                className="w-6 h-6 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-900">GP Diagnostik</h1>
              <p className="text-xs text-gray-500">
                Klinisk beslutningsstøtte · PLO &amp; DSAM retningslinjer
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <span className="w-2 h-2 bg-green-500 rounded-full inline-block"></span>
            Kun til klinisk støtte – ikke erstatning for lægelig vurdering
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* LEFT: Patient Form */}
          <div className="space-y-4">
            <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
              <div className="px-5 py-4 bg-blue-700 text-white">
                <h2 className="font-semibold text-base">
                  Patientoplysninger
                </h2>
                <p className="text-blue-200 text-xs mt-0.5">
                  Udfyld patient- og symptomdata
                </p>
              </div>

              <form onSubmit={handleSubmit} className="p-5 space-y-4">
                {/* Alder og Køn */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Alder <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <input
                        type="number"
                        name="alder"
                        value={form.alder}
                        onChange={handleChange}
                        min={0}
                        max={120}
                        placeholder="fx 52"
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none pr-10"
                      />
                      <span className="absolute right-3 top-2.5 text-gray-400 text-xs">
                        år
                      </span>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Biologisk køn <span className="text-red-500">*</span>
                    </label>
                    <div className="flex gap-3 mt-2">
                      {[
                        { value: "mand", label: "Mand" },
                        { value: "kvinde", label: "Kvinde" },
                      ].map((opt) => (
                        <label
                          key={opt.value}
                          className="flex items-center gap-1.5 cursor-pointer"
                        >
                          <input
                            type="radio"
                            name="kon"
                            value={opt.value}
                            checked={form.kon === opt.value}
                            onChange={handleChange}
                            className="text-blue-600"
                          />
                          <span className="text-sm text-gray-700">
                            {opt.label}
                          </span>
                        </label>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Symptomer */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Aktuelle symptomer{" "}
                    <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    name="symptomer"
                    value={form.symptomer}
                    onChange={handleChange}
                    rows={4}
                    placeholder="Beskriv symptomerne detaljeret: lokalisation, karakter, intensitet (NRS 0-10), forværrende/lindrende faktorer, ledsagesymptomer..."
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                  />
                </div>

                {/* Varighed */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Varighed <span className="text-red-500">*</span>
                  </label>
                  <select
                    name="varighed"
                    value={form.varighed}
                    onChange={handleChange}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none bg-white"
                  >
                    <option value="">Vælg varighed...</option>
                    {varighedOptions.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Anamnese */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Relevant anamnese
                    <span className="text-gray-400 font-normal ml-1 text-xs">
                      (valgfri)
                    </span>
                  </label>
                  <textarea
                    name="anamnese"
                    value={form.anamnese}
                    onChange={handleChange}
                    rows={3}
                    placeholder="Tidligere sygdomme, operationer, relevante fund ved objektiv undersøgelse (BT, puls, temp, saturation, vægt/BMI)..."
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                  />
                </div>

                {/* Medicin */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Aktuel medicin
                    <span className="text-gray-400 font-normal ml-1 text-xs">
                      (valgfri)
                    </span>
                  </label>
                  <textarea
                    name="medicin"
                    value={form.medicin}
                    onChange={handleChange}
                    rows={2}
                    placeholder="Fx: Metformin 1000 mg x2, Ramipril 5 mg x1, Atorvastatin 40 mg x1..."
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                  />
                </div>

                {/* Allergier */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Allergier
                    <span className="text-gray-400 font-normal ml-1 text-xs">
                      (valgfri)
                    </span>
                  </label>
                  <input
                    type="text"
                    name="allergier"
                    value={form.allergier}
                    onChange={handleChange}
                    placeholder="Fx: Penicillin, sulfonamider..."
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                  />
                </div>

                {error && (
                  <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
                    {error}
                  </div>
                )}

                {/* Buttons */}
                <div className="flex gap-3 pt-2">
                  <button
                    type="submit"
                    disabled={loading}
                    className="flex-1 bg-blue-700 hover:bg-blue-800 disabled:bg-blue-400 text-white font-medium py-2.5 px-4 rounded-lg transition-colors text-sm flex items-center justify-center gap-2"
                  >
                    {loading ? (
                      <>
                        <svg
                          className="animate-spin w-4 h-4"
                          fill="none"
                          viewBox="0 0 24 24"
                        >
                          <circle
                            className="opacity-25"
                            cx="12"
                            cy="12"
                            r="10"
                            stroke="currentColor"
                            strokeWidth="4"
                          />
                          <path
                            className="opacity-75"
                            fill="currentColor"
                            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                          />
                        </svg>
                        Analyserer...
                      </>
                    ) : (
                      <>
                        <svg
                          className="w-4 h-4"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
                          />
                        </svg>
                        Generer diagnostik
                      </>
                    )}
                  </button>
                  {(loading || result) && (
                    <button
                      type="button"
                      onClick={loading ? handleStop : handleReset}
                      className="px-4 py-2.5 border border-gray-300 hover:border-gray-400 text-gray-700 font-medium rounded-lg transition-colors text-sm"
                    >
                      {loading ? "Stop" : "Ryd"}
                    </button>
                  )}
                </div>
              </form>
            </div>

            {/* Disclaimer */}
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 flex gap-2">
              <svg
                className="w-4 h-4 text-amber-600 mt-0.5 flex-shrink-0"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fillRule="evenodd"
                  d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
              <p className="text-xs text-amber-800">
                <strong>Klinisk beslutningsstøtte:</strong> Dette værktøj er kun
                til brug som supplement til den kliniske vurdering. Den
                behandlende læge er altid ansvarlig for den endelige beslutning.
                Baseret på PLO, DSAM og Sundhedsstyrelsen retningslinjer.
              </p>
            </div>
          </div>

          {/* RIGHT: Diagnostic Result */}
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
            <div className="px-5 py-4 bg-gray-800 text-white flex items-center justify-between">
              <div>
                <h2 className="font-semibold text-base">
                  Diagnostisk vurdering
                </h2>
                <p className="text-gray-400 text-xs mt-0.5">
                  AI-genereret analyse · PLO best practice
                </p>
              </div>
              {done && result && (
                <span className="text-xs bg-green-600 text-white px-2 py-0.5 rounded-full">
                  Færdig
                </span>
              )}
              {loading && (
                <span className="text-xs bg-blue-500 text-white px-2 py-0.5 rounded-full animate-pulse">
                  Genererer...
                </span>
              )}
            </div>

            <div className="flex-1 p-5 overflow-y-auto min-h-[500px] max-h-[700px]">
              {!result && !loading && (
                <div className="h-full flex flex-col items-center justify-center text-center text-gray-400 gap-4">
                  <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center">
                    <svg
                      className="w-8 h-8 text-gray-300"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      />
                    </svg>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-500">
                      Ingen analyse endnu
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      Udfyld patientdata og tryk &quot;Generer diagnostik&quot;
                    </p>
                  </div>
                  <div className="grid grid-cols-2 gap-2 mt-2 w-full max-w-xs">
                    {[
                      "Primær diagnose (ICPC-2)",
                      "Differentialdiagnoser",
                      "Udredning & prøver",
                      "Behandling",
                      "Røde flag",
                      "Opfølgning",
                    ].map((item) => (
                      <div
                        key={item}
                        className="bg-gray-50 border border-gray-100 rounded px-2 py-1 text-xs text-gray-400"
                      >
                        {item}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {(result || loading) && (
                <div
                  className={`diagnostic-output prose prose-sm max-w-none ${
                    loading && !done ? "cursor-blink" : ""
                  }`}
                >
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {result}
                  </ReactMarkdown>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <footer className="mt-8 text-center text-xs text-gray-400 pb-4">
          <p>
            GP Diagnostik · Baseret på PLO, DSAM og Sundhedsstyrelsens
            retningslinjer · Kun til klinisk beslutningsstøtte
          </p>
        </footer>
      </main>
    </div>
  );
}
