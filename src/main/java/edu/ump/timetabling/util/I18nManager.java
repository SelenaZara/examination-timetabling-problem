package edu.ump.timetabling.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages internationalization (i18n) properties bundles (en, ms, zh).
 */
public class I18nManager {

    private final Map<String, Map<String, String>> bundles = new HashMap<>();

    public I18nManager() {
        loadLanguage("en");
        loadLanguage("ms");
        loadLanguage("zh");
    }

    private void loadLanguage(String lang) {
        Map<String, String> map = new HashMap<>();
        File file = new File("src/main/resources/i18n/messages_" + lang + ".properties");
        if (!file.exists()) {
            file = new File("resources/i18n/messages_" + lang + ".properties");
        }

        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                for (String key : props.stringPropertyNames()) {
                    map.put(key, props.getProperty(key));
                }
            } catch (Exception e) {
                populateFallback(lang, map);
            }
        } else {
            String resourcePath = "/i18n/messages_" + lang + ".properties";
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                    for (String key : props.stringPropertyNames()) {
                        map.put(key, props.getProperty(key));
                    }
                } else {
                    populateFallback(lang, map);
                }
            } catch (Exception e) {
                populateFallback(lang, map);
            }
        }

        // Ensure new capability and visitor keys exist
        populateFallbackDefaults(lang, map);
        bundles.put(lang, map);
    }

    private void populateFallback(String lang, Map<String, String> map) {
        if ("ms".equals(lang)) {
            map.put("app.title", "Sistem Penjadualan Peperiksaan Akademik");
            map.put("app.subtitle", "Penyelidikan Pengoptimuman Jadual Waktu Peperiksaan");
            map.put("control.initialize", "Prapemulaan");
            map.put("control.setting", "Tetapan");
            map.put("conflict.title", "Penjana & Pemeriksa Matriks Konflik (Conflict Matrix)");
            map.put("conflict.desc", "Matriks konflik dibina mengikut susunan kursus berdasarkan pendaftaran pelajar");
            map.put("conflict.generate_btn", "Jana Matriks Konflik");
            map.put("conflict.export_btn", "Eksport Matriks Konflik (.CSV)");
            map.put("conflict.upload_excel", "Muat Naik Dataset (Excel / CSV)");
            map.put("initial.phase", "Fasa Permulaan");
            map.put("initial.generate", "Jana");
            map.put("improve.phase", "Fasa Penambahbaikan");
            map.put("improve.generate", "Jana");
            map.put("timetable.xit", "Jadual Waktu Xit");
            map.put("timetable.yir", "Jadual Waktu Yir");
            map.put("timetable.zrt", "Jadual Waktu Zrt");
            map.put("timetable.calculate", "Kira");
            map.put("output.title", "Perbandingan Algoritma & Log Keputusan");
            map.put("table.course_code", "Kod Kursus");
            map.put("table.course_name", "Nama Kursus");
            map.put("table.enrollment", "Pelajar");
            map.put("table.period", "Slot / Waktu (t)");
            map.put("table.room", "Bilik / Dewan (r)");
            map.put("table.campus", "Kampus");
            map.put("table.conflicts", "Konflik");
            map.put("table.penalty", "Penalti Lembut");
        } else if ("zh".equals(lang)) {
            map.put("app.title", "学术考试排程优化系统");
            map.put("app.subtitle", "考试时间表优化与元启发式算法研究平台");
            map.put("control.initialize", "初始化");
            map.put("control.setting", "参数设置");
            map.put("conflict.title", "冲突矩阵构建与分析 (Conflict Matrix)");
            map.put("conflict.desc", "按照课程顺序与学生选课重叠关系构建对称冲突矩阵");
            map.put("conflict.generate_btn", "生成冲突矩阵");
            map.put("conflict.export_btn", "导出冲突矩阵 (.CSV)");
            map.put("conflict.upload_excel", "上传数据集 (Excel / CSV)");
            map.put("initial.phase", "初始解构造阶段");
            map.put("initial.generate", "生成");
            map.put("improve.phase", "改进优化阶段");
            map.put("improve.generate", "生成");
            map.put("timetable.xit", "Xit 时间表");
            map.put("timetable.yir", "Yir 时间表");
            map.put("timetable.zrt", "Zrt 时间表");
            map.put("timetable.calculate", "计算评估");
            map.put("output.title", "算法对比与运行日志");
            map.put("table.course_code", "课程代码");
            map.put("table.course_name", "课程名称");
            map.put("table.enrollment", "考生人数");
            map.put("table.period", "时段 (t)");
            map.put("table.room", "考场 (r)");
            map.put("table.campus", "校区");
            map.put("table.conflicts", "冲突数");
            map.put("table.penalty", "软约束惩罚");
        } else {
            map.put("app.title", "Academic Examination Timetabling System");
            map.put("app.subtitle", "Examination Timetabling Optimization Research Platform");
            map.put("control.initialize", "Initialize");
            map.put("control.setting", "Setting");
            map.put("conflict.title", "Conflict Matrix Generator & Inspector");
            map.put("conflict.desc", "Conflict matrix structured according to course sequence based on student clash registrations");
            map.put("conflict.generate_btn", "Generate Conflict Matrix");
            map.put("conflict.export_btn", "Export Conflict Matrix (.CSV)");
            map.put("conflict.upload_excel", "Upload Dataset (Excel / CSV)");
            map.put("initial.phase", "Initial Phase");
            map.put("initial.generate", "Generate");
            map.put("improve.phase", "Improvement Phase");
            map.put("improve.generate", "Generate");
            map.put("timetable.xit", "Xit Timetable");
            map.put("timetable.yir", "Yir Timetable");
            map.put("timetable.zrt", "Zrt Timetable");
            map.put("timetable.calculate", "Generate Timetable");
            map.put("output.title", "Algorithm Comparison & Execution Output");
            map.put("table.course_code", "Course Code");
            map.put("table.course_name", "Course Name");
            map.put("table.enrollment", "Enrollment");
            map.put("table.period", "Period (t)");
            map.put("table.room", "Room (r)");
            map.put("table.campus", "Campus");
            map.put("table.conflicts", "Conflicts");
            map.put("table.penalty", "Soft Penalty");
        }
    }

    private void populateFallbackDefaults(String lang, Map<String, String> map) {
        if ("ms".equals(lang)) {
            map.putIfAbsent("capabilities.badge", "Keupayaan Sistem");
            map.putIfAbsent("capabilities.title", "Apa Yang Laman Web Ini Boleh Lakukan");
            map.putIfAbsent("capabilities.subtitle", "Enjin Penyelidikan & Pengoptimuman Jadual Waktu Peperiksaan Multi-Kampus");
            map.putIfAbsent("capabilities.quick_demo", "⚡ Demo Pantas");
            map.putIfAbsent("capabilities.guide_btn", "📖 Panduan Sistem");
            map.putIfAbsent("nav.capabilities", "Keupayaan Laman Web");
            map.putIfAbsent("nav.visitors", "Pelawat");
            map.putIfAbsent("footer.visitors", "Jumlah Lawatan Platform:");
            map.putIfAbsent("footer.uptime", "Masa Beroperasi:");
            map.putIfAbsent("footer.status", "Dalam Talian");
        } else if ("zh".equals(lang)) {
            map.putIfAbsent("capabilities.badge", "系统功能与特性");
            map.putIfAbsent("capabilities.title", "本平台的功能与特性");
            map.putIfAbsent("capabilities.subtitle", "全方位多校区大学考试时间表排程与元启发式算法优化引擎");
            map.putIfAbsent("capabilities.quick_demo", "⚡ 快速演示");
            map.putIfAbsent("capabilities.guide_btn", "📖 架构与用户指南");
            map.putIfAbsent("nav.capabilities", "功能特性");
            map.putIfAbsent("nav.visitors", "访客");
            map.putIfAbsent("footer.visitors", "平台总访问量:");
            map.putIfAbsent("footer.uptime", "运行时间:");
            map.putIfAbsent("footer.status", "在线");
        } else {
            map.putIfAbsent("capabilities.badge", "System Capabilities");
            map.putIfAbsent("capabilities.title", "What This Website Can Do");
            map.putIfAbsent("capabilities.subtitle", "Comprehensive Multi-Campus Examination Scheduling & Optimization Research Engine");
            map.putIfAbsent("capabilities.quick_demo", "⚡ Quick Demo");
            map.putIfAbsent("capabilities.guide_btn", "📖 System Guide");
            map.putIfAbsent("nav.capabilities", "What This Website Can Do");
            map.putIfAbsent("nav.visitors", "Visitors");
            map.putIfAbsent("footer.visitors", "Total Platform Visits:");
            map.putIfAbsent("footer.uptime", "Uptime:");
            map.putIfAbsent("footer.status", "Online");
        }
    }

    public Map<String, String> getBundle(String lang) {
        String key = (lang != null && bundles.containsKey(lang.toLowerCase())) ? lang.toLowerCase() : "en";
        return bundles.get(key);
    }
}
