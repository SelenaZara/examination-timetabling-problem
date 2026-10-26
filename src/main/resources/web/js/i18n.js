/**
 * Internationalization (i18n) Module for Timetabling Web GUI.
 * Dynamically swaps UI text dictionaries without reloading the webpage.
 */
const I18n = {
    currentLang: 'en',
    dictionaries: {},

    async init() {
        await this.loadLanguage(this.currentLang);
    },

    async setLanguage(lang) {
        if (!lang) return;
        this.currentLang = lang;
        await this.loadLanguage(lang);
        this.applyTranslations();
    },

    async loadLanguage(lang) {
        try {
            const resp = await fetch(`/api/i18n?lang=${lang}`);
            if (resp.ok) {
                const data = await resp.json();
                this.dictionaries[lang] = data;
            }
        } catch (e) {
            console.warn("Failed to fetch i18n dictionary for", lang, e);
        }
    },

    t(key, fallback = '') {
        const dict = this.dictionaries[this.currentLang];
        if (dict && dict[key]) {
            return dict[key];
        }
        return fallback || key;
    },

    applyTranslations() {
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const translation = this.t(key);
            if (translation) {
                el.textContent = translation;
            }
        });

        document.querySelectorAll('[data-i18n-title]').forEach(el => {
            const key = el.getAttribute('data-i18n-title');
            const translation = this.t(key);
            if (translation) {
                el.title = translation;
            }
        });
    }
};

window.I18n = I18n;
