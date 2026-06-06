package com.smartload.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.smartload.dto.CgResultDto;
import com.smartload.dto.LoadPlanResultDto;
import com.smartload.dto.PackagePlacementDto;
import com.smartload.dto.UldAssignmentDto;
import com.smartload.entity.Aircraft;
import com.smartload.entity.Manifest;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PdfReportService — Faz 5 raporlama.
 *
 * SmartLoad'un iki resmi PDF çıktısını OpenPDF (com.lowagie) ile programatik
 * olarak üretir:
 *   1. LIR (Load Instruction Report) — yer ekibine verilen yükleme talimatı.
 *   2. Load Sheet — uçağın ağırlık & denge (W&B) tablosu, kokpite teslim edilir.
 *
 * Tasarım kararları:
 *   - ADR-0019: PDF kütüphanesi OpenPDF (LGPL, lisans güvenli; iText 7 AGPL değil).
 *   - ADR-0020: Programatik üretim (fillable template değil) — esneklik için.
 *   - Font: Helvetica + Cp1254 (Türkçe Windows) kodlaması → loadmaster isimlerinde
 *     ç, ğ, ı, ö, ş, ü karakterleri doğru render edilir; TTF embed gerekmez.
 *
 * Akademik dürüstlük notu: gerçek AHM560 / fuel verisi yok. DOW = OEW (mock),
 * fuel kapsam dışı → ZFW raporlanır, TOW/LDW yakıt girilmediği için "—" gösterilir.
 */
@Service
public class PdfReportService {

    // ── Renk paleti ───────────────────────────────────────────────────────────
    private static final Color BRAND      = new Color(13, 110, 253);   // bootstrap primary
    private static final Color DARK        = new Color(33, 37, 41);
    private static final Color GREY        = new Color(108, 117, 125);
    private static final Color LIGHT_GREY  = new Color(233, 236, 239);
    private static final Color GREEN       = new Color(25, 135, 84);
    private static final Color YELLOW      = new Color(255, 193, 7);
    private static final Color RED         = new Color(220, 53, 69);

    private static final DateTimeFormatter DT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Fontlar (Cp1254 = Türkçe karakter desteği) ────────────────────────────
    private final BaseFont base;

    public PdfReportService() {
        try {
            this.base = BaseFont.createFont(BaseFont.HELVETICA, "Cp1254", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("PDF font yüklenemedi", e);
        }
    }

    private Font f(float size, int style, Color color) {
        return new Font(base, size, style, color);
    }

    // ══ LIR ════════════════════════════════════════════════════════════════════

    /**
     * Load Instruction Report üretir.
     *
     * @param dto      tam load plan sonucu (assignments + cg)
     * @param aircraft uçak (tip/icao)
     * @param manifest kaynak manifest (dosya adı, toplamlar)
     * @return PDF byte dizisi (application/pdf)
     */
    public byte[] generateLir(LoadPlanResultDto dto, Aircraft aircraft, Manifest manifest) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 54, 48);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new Footer("LOAD INSTRUCTION REPORT — SmartLoad"));
        doc.open();

        addReportHeader(doc, "LOAD INSTRUCTION REPORT (LIR)", dto, aircraft, manifest);

        // ── Pozisyon talimat tablosu (sadece dolu pozisyonlar, LOFO sıralı) ─────
        List<UldAssignmentDto> loaded = dto.getAssignments().stream()
            .filter(a -> a.getPackageCount() > 0)
            .sorted(Comparator.comparing(
                a -> a.getLoadingOrder() != null ? a.getLoadingOrder() : Integer.MAX_VALUE))
            .toList();

        sectionTitle(doc, "LOADING INSTRUCTIONS  (load sequence: 1 = first / deepest)");

        PdfPTable t = new PdfPTable(new float[]{ 1.4f, 1.1f, 2.0f, 1.1f, 1.5f, 1.2f, 1.4f, 1.0f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        headerRow(t, "SEQ", "POS", "ULD ID / TYPE", "PKGS", "GROSS WT", "UTIL", "DEST", "DG");

        for (UldAssignmentDto a : loaded) {
            String seq = a.getLoadingOrder() != null ? String.valueOf(a.getLoadingOrder()) : "—";
            String uldId = "SL-" + dto.getId() + "-" + a.getPositionCode()
                         + "  (" + a.getUldTypeCode() + ")";
            cell(t, seq, Element.ALIGN_CENTER);
            cell(t, a.getPositionCode(), Element.ALIGN_CENTER);
            cell(t, uldId, Element.ALIGN_LEFT);
            cell(t, String.valueOf(a.getPackageCount()), Element.ALIGN_CENTER);
            cell(t, String.format("%.1f kg", a.getTotalWeightKg()), Element.ALIGN_RIGHT);
            cell(t, String.format("%.0f%%", a.getUtilizationPct()), Element.ALIGN_RIGHT);
            cell(t, a.getDominantDestination() != null ? a.getDominantDestination() : "—",
                 Element.ALIGN_CENTER);
            cell(t, specialHandlingFlags(a), Element.ALIGN_CENTER);
        }
        doc.add(t);

        // ── Special handling özet ──────────────────────────────────────────────
        Set<String> sh = new LinkedHashSet<>();
        for (UldAssignmentDto a : loaded) {
            for (PackagePlacementDto p : a.getPlacements()) {
                if (p.getSpecialHandling() != null && !p.getSpecialHandling().isBlank()) {
                    for (String code : p.getSpecialHandling().split("[,;/ ]+")) {
                        if (!code.isBlank()) sh.add(code.trim().toUpperCase());
                    }
                }
                if (p.getDgClass() != null && !p.getDgClass().isBlank()) {
                    sh.add("DG-" + p.getDgClass().trim());
                }
            }
        }
        if (!sh.isEmpty()) {
            sectionTitle(doc, "SPECIAL HANDLING NOTES");
            Paragraph note = new Paragraph(String.join("   •   ", sh), f(10, Font.BOLD, RED));
            note.setSpacingBefore(2);
            doc.add(note);
        }

        addSummaryBlock(doc, dto);
        addSignatureBlock(doc,
            new String[]{ "Prepared by (Loadmaster)", "Ground Crew Acknowledgement" });

        doc.close();
        return out.toByteArray();
    }

    // ══ LOAD SHEET ═══════════════════════════════════════════════════════════════

    /**
     * Load Sheet (ağırlık & denge) üretir.
     */
    public byte[] generateLoadSheet(LoadPlanResultDto dto, Aircraft aircraft, Manifest manifest) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 54, 48);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new Footer("LOAD SHEET (WEIGHT & BALANCE) — SmartLoad"));
        doc.open();

        addReportHeader(doc, "LOAD SHEET — WEIGHT & BALANCE", dto, aircraft, manifest);

        CgResultDto cg = dto.getCg();
        double oew     = cg != null ? cg.getOewKg() : CgResultDto.OEW_KG;
        double cargo   = dto.getTotalWeightKg();
        double zfw     = oew + cargo;

        // ── Ağırlık özeti (DOW/ZFW/TOW/LDW) ────────────────────────────────────
        sectionTitle(doc, "WEIGHT SUMMARY");
        PdfPTable w = new PdfPTable(new float[]{ 3f, 2f, 3f });
        w.setWidthPercentage(70);
        w.setHorizontalAlignment(Element.ALIGN_LEFT);
        w.setSpacingBefore(4);
        headerRow(w, "ITEM", "WEIGHT (kg)", "NOTE");
        kvRow(w, "DOW  (Dry Operating Weight)", String.format("%,.0f", oew), "mock OEW (akademik)");
        kvRow(w, "Payload (cargo)",             String.format("%,.0f", cargo), dto.getPlacedPackages() + " pkgs");
        kvRow(w, "ZFW  (Zero Fuel Weight)",     String.format("%,.0f", zfw),   "DOW + payload");
        kvRow(w, "TOW  (Take-Off Weight)",      "—", "yakıt verisi kapsam dışı");
        kvRow(w, "LDW  (Landing Weight)",       "—", "yakıt verisi kapsam dışı");
        doc.add(w);

        // ── CG / denge ──────────────────────────────────────────────────────────
        if (cg != null) {
            sectionTitle(doc, "BALANCE  (CG / %MAC)");
            PdfPTable c = new PdfPTable(new float[]{ 3f, 2f, 3f });
            c.setWidthPercentage(70);
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            c.setSpacingBefore(4);
            headerRow(c, "ITEM", "VALUE", "STATUS");
            kvRow(c, "ZFW CG arm", String.format("%.0f mm", cg.getCgArmMm()), "scene coords");
            PdfPCell macCell = valueCell(String.format("%.1f %%MAC", cg.getCgMacPct()));
            c.addCell(labelCell("ZFW CG (%MAC)"));
            c.addCell(macCell);
            c.addCell(statusCell(cg.getStatus()));
            kvRow(c, "Envelope limits", String.format("%.0f–%.0f %%MAC",
                cg.getFwdLimitPct(), cg.getAftLimitPct()), "fwd / aft hard limits");
            doc.add(c);
        }

        // ── Moment tablosu (pozisyon bazlı) ────────────────────────────────────
        sectionTitle(doc, "MOMENT TABLE  (per position)");
        PdfPTable m = new PdfPTable(new float[]{ 2f, 2f, 2f, 3f });
        m.setWidthPercentage(85);
        m.setHorizontalAlignment(Element.ALIGN_LEFT);
        m.setSpacingBefore(4);
        headerRow(m, "POSITION", "WEIGHT (kg)", "ARM (mm)", "MOMENT (kg·mm)");

        // OEW satırı
        double oewArm = cg != null ? cg.getOewArmMm() : CgResultDto.OEW_ARM_MM;
        momentRow(m, "OEW (aircraft)", oew, oewArm, true);

        double cargoMoment = 0;
        for (UldAssignmentDto a : dto.getAssignments()) {
            if (a.getPackageCount() > 0 && a.getTotalWeightKg() > 0) {
                momentRow(m, a.getPositionCode(), a.getTotalWeightKg(), a.getArmMm(), false);
                cargoMoment += a.getTotalWeightKg() * a.getArmMm();
            }
        }
        // Toplam satırı
        double totalMoment = oew * oewArm + cargoMoment;
        PdfPCell tot = new PdfPCell(new Phrase("TOTAL (ZFW)", f(9, Font.BOLD, DARK)));
        tot.setBackgroundColor(LIGHT_GREY); tot.setPadding(4);
        m.addCell(tot);
        m.addCell(boldNum(String.format("%,.0f", zfw)));
        m.addCell(boldNum(cg != null ? String.format("%.0f", cg.getCgArmMm()) : "—"));
        m.addCell(boldNum(String.format("%,.0f", totalMoment)));
        doc.add(m);

        addSignatureBlock(doc,
            new String[]{ "Loadmaster", "Captain", "Dispatcher" });

        doc.close();
        return out.toByteArray();
    }

    // ══ Ortak bloklar ════════════════════════════════════════════════════════════

    private void addReportHeader(Document doc, String title, LoadPlanResultDto dto,
                                 Aircraft aircraft, Manifest manifest) {
        // Başlık şeridi
        PdfPTable head = new PdfPTable(new float[]{ 3f, 2f });
        head.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        Paragraph brand = new Paragraph("SmartLoad", f(18, Font.BOLD, BRAND));
        Paragraph sub   = new Paragraph("Boeing 777F Cargo Load Optimization", f(8, Font.NORMAL, GREY));
        left.addElement(brand);
        left.addElement(sub);
        head.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph t = new Paragraph(title, f(13, Font.BOLD, DARK));
        t.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(t);
        String created = dto.getCreatedAt() != null ? dto.getCreatedAt().format(DT) : "";
        Paragraph pid = new Paragraph("Plan #" + dto.getId() + "   ·   " + created,
            f(8, Font.NORMAL, GREY));
        pid.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(pid);
        head.addCell(right);
        doc.add(head);

        // Ayraç
        doc.add(divider());

        // Bilgi şeridi (flight / aircraft / manifest)
        PdfPTable info = new PdfPTable(4);
        info.setWidthPercentage(100);
        info.setSpacingBefore(6);
        infoCell(info, "AIRCRAFT", aircraft != null ? aircraft.getName() : "Boeing 777F");
        infoCell(info, "TYPE", aircraft != null ? aircraft.getIcaoCode() : "B77F");
        infoCell(info, "MANIFEST",
            manifest != null && manifest.getFileName() != null
                ? manifest.getFileName() : dto.getManifestId());
        infoCell(info, "ALGORITHM", dto.getAlgorithm() != null ? dto.getAlgorithm() : "—");
        doc.add(info);
        doc.add(new Paragraph(" ", f(4, Font.NORMAL, DARK)));
    }

    private void addSummaryBlock(Document doc, LoadPlanResultDto dto) {
        sectionTitle(doc, "SUMMARY");
        PdfPTable s = new PdfPTable(4);
        s.setWidthPercentage(100);
        s.setSpacingBefore(4);
        infoCell(s, "PACKAGES PLACED",
            dto.getPlacedPackages() + " / " + dto.getTotalPackages());
        infoCell(s, "TOTAL CARGO WEIGHT", String.format("%,.0f kg", dto.getTotalWeightKg()));
        infoCell(s, "ULD POSITIONS USED", String.valueOf(dto.getUsedPositions()));
        CgResultDto cg = dto.getCg();
        infoCell(s, "CG (%MAC)",
            cg != null ? String.format("%.1f%%  (%s)", cg.getCgMacPct(), cg.getStatus()) : "—");
        doc.add(s);
    }

    private void addSignatureBlock(Document doc, String[] roles) {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(24);
        doc.add(spacer);

        PdfPTable sig = new PdfPTable(roles.length);
        sig.setWidthPercentage(100);
        for (String role : roles) {
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.NO_BORDER);
            c.setPaddingTop(18);
            Paragraph line = new Paragraph("______________________________", f(10, Font.NORMAL, DARK));
            Paragraph lbl  = new Paragraph(role + "   /   Date", f(8, Font.NORMAL, GREY));
            lbl.setSpacingBefore(2);
            c.addElement(line);
            c.addElement(lbl);
            sig.addCell(c);
        }
        doc.add(sig);
    }

    // ══ Yardımcılar ══════════════════════════════════════════════════════════════

    private Paragraph divider() {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2);
        LineSeparator ls = new LineSeparator(1f, 100f, BRAND, Element.ALIGN_CENTER, -2);
        p.add(new Chunk(ls));
        return p;
    }

    private void sectionTitle(Document doc, String text) {
        Paragraph p = new Paragraph(text, f(10, Font.BOLD, BRAND));
        p.setSpacingBefore(14);
        p.setSpacingAfter(0);
        doc.add(p);
    }

    private void headerRow(PdfPTable t, String... headers) {
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, f(8, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(DARK);
            c.setPadding(4);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
    }

    private void cell(PdfPTable t, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f(8, Font.NORMAL, DARK)));
        c.setPadding(3);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void kvRow(PdfPTable t, String k, String v, String note) {
        t.addCell(labelCell(k));
        t.addCell(valueCell(v));
        PdfPCell n = new PdfPCell(new Phrase(note, f(8, Font.ITALIC, GREY)));
        n.setPadding(4);
        t.addCell(n);
    }

    private PdfPCell labelCell(String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, f(9, Font.NORMAL, DARK)));
        c.setPadding(4);
        return c;
    }

    private PdfPCell valueCell(String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, f(9, Font.BOLD, DARK)));
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private PdfPCell statusCell(String status) {
        Color col = status == null ? GREY
            : status.startsWith("RED") ? RED
            : status.startsWith("YELLOW") ? YELLOW
            : GREEN;
        Color fg = col.equals(YELLOW) ? DARK : Color.WHITE;
        PdfPCell c = new PdfPCell(new Phrase(status != null ? status : "—", f(8, Font.BOLD, fg)));
        c.setBackgroundColor(col);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private void momentRow(PdfPTable t, String pos, double weight, double arm, boolean oew) {
        Font ft = f(8, oew ? Font.ITALIC : Font.NORMAL, oew ? GREY : DARK);
        PdfPCell p = new PdfPCell(new Phrase(pos, ft)); p.setPadding(3);
        PdfPCell w = new PdfPCell(new Phrase(String.format("%,.1f", weight), ft));
        w.setPadding(3); w.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell a = new PdfPCell(new Phrase(String.format("%,.0f", arm), ft));
        a.setPadding(3); a.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell mo = new PdfPCell(new Phrase(String.format("%,.0f", weight * arm), ft));
        mo.setPadding(3); mo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(p); t.addCell(w); t.addCell(a); t.addCell(mo);
    }

    private PdfPCell boldNum(String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, f(9, Font.BOLD, DARK)));
        c.setBackgroundColor(LIGHT_GREY);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private void infoCell(PdfPTable t, String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(LIGHT_GREY);
        c.setPadding(6);
        Paragraph l = new Paragraph(label, f(7, Font.NORMAL, GREY));
        Paragraph v = new Paragraph(value, f(10, Font.BOLD, DARK));
        c.addElement(l);
        c.addElement(v);
        t.addCell(c);
    }

    private String specialHandlingFlags(UldAssignmentDto a) {
        boolean dg = a.getPlacements().stream()
            .anyMatch(p -> (p.getDgClass() != null && !p.getDgClass().isBlank())
                || (p.getSpecialHandling() != null
                    && p.getSpecialHandling().toUpperCase().contains("DG")));
        return dg ? "DG" : "—";
    }

    // ── Footer (sayfa altı çizgi + sayfa no) ──────────────────────────────────
    private class Footer extends PdfPageEventHelper {
        private final String label;
        Footer(String label) { this.label = label; }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            Rectangle rect = doc.getPageSize();
            Phrase left = new Phrase(label, f(7, Font.NORMAL, GREY));
            Phrase right = new Phrase(
                "Generated " + java.time.LocalDateTime.now().format(DT)
                    + "   ·   page " + writer.getPageNumber(),
                f(7, Font.NORMAL, GREY));
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                writer.getDirectContent(), Element.ALIGN_LEFT, left, rect.getLeft(36), rect.getBottom(30), 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                writer.getDirectContent(), Element.ALIGN_RIGHT, right, rect.getRight(36), rect.getBottom(30), 0);
        }
    }
}
