// ===========================
// CERTIFICATE GENERATOR APP
// ===========================
// This file handles two things:
//   1. Live preview — update the certificate as the user types
//   2. PDF download — capture the certificate and save it as a PDF

// ---------- GRAB ELEMENTS ----------

// Form inputs
const recipientInput = document.getElementById("recipientName");
const courseInput = document.getElementById("courseName");
const dateInput = document.getElementById("issueDate");
const issuerInput = document.getElementById("issuerName");

// Certificate display elements
const displayName = document.getElementById("displayName");
const displayCourse = document.getElementById("displayCourse");
const displayDate = document.getElementById("displayDate");
const displayIssuer = document.getElementById("displayIssuer");

// Buttons
const downloadBtn = document.getElementById("downloadBtn");

// ---------- SET DEFAULT DATE ----------

// Set today's date as the default value for the date picker
const today = new Date();
dateInput.value = today.toISOString().split("T")[0];

// ---------- HELPER: FORMAT DATE ----------

function formatDate(dateString) {
  // Turn "2026-02-08" into "February 8, 2026"
  const date = new Date(dateString + "T00:00:00"); // avoid timezone shift
  const options = { year: "numeric", month: "long", day: "numeric" };
  return date.toLocaleDateString("en-US", options);
}

// ---------- LIVE PREVIEW ----------

// Every time the user types, update the certificate instantly

recipientInput.addEventListener("input", function () {
  displayName.textContent = recipientInput.value || "Jane Doe";
});

courseInput.addEventListener("input", function () {
  displayCourse.textContent = courseInput.value || "Introduction to Web Development";
});

dateInput.addEventListener("input", function () {
  displayDate.textContent = dateInput.value
    ? formatDate(dateInput.value)
    : "February 8, 2026";
});

issuerInput.addEventListener("input", function () {
  displayIssuer.textContent = issuerInput.value || "Code Academy";
});

// ---------- PDF DOWNLOAD ----------

downloadBtn.addEventListener("click", function () {
  const certificate = document.getElementById("certificate");

  // Step 1: Use html2canvas to turn the certificate div into an image
  html2canvas(certificate, { scale: 2, useCORS: true }).then(function (canvas) {
    // Step 2: Convert the canvas to a JPEG image
    const imgData = canvas.toDataURL("image/jpeg", 1.0);

    // Step 3: Create a PDF in landscape orientation (matches certificate shape)
    const { jsPDF } = window.jspdf;
    const pdf = new jsPDF({
      orientation: "landscape",
      unit: "px",
      format: [canvas.width, canvas.height],
    });

    // Step 4: Add the image to the PDF
    pdf.addImage(imgData, "JPEG", 0, 0, canvas.width, canvas.height);

    // Step 5: Download! Use the recipient name in the filename
    const name = recipientInput.value || "certificate";
    pdf.save(name.replace(/\s+/g, "_") + "_certificate.pdf");
  });
});
