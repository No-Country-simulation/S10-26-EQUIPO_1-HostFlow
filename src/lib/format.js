export const cn = (...items) => items.filter(Boolean).join(" ")

export const isEmail = (value = "") => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value.trim())
