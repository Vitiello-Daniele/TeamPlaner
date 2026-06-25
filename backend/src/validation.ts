// Prüfregeln für Termine. Werden im Backend genutzt,
// damit dieselben Regeln wie in der App auch serverseitig gelten.

// Prüft, ob das Datum wirklich existiert (z. B. 31.02. ist ungültig)
export function isValidEventDate(date: string): boolean {
  // Format muss TT.MM.JJJJ sein
  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(date);

  if (!match) {
    return false;
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const parsedDate = new Date(Date.UTC(year, month - 1, day));

  return parsedDate.getUTCFullYear() === year &&
    parsedDate.getUTCMonth() === month - 1 &&
    parsedDate.getUTCDate() === day;
}

// Prüft, ob das Datum in der Vergangenheit liegt (Termine sollen nur in der Zukunft sein)
export function isPastEventDate(date: string, now = new Date()): boolean {
  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(date);

  if (!match) {
    return false;
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const selectedDate = new Date(Date.UTC(year, month - 1, day));
  const today = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));

  return selectedDate < today;
}

// Prüft die Uhrzeit im 24-Stunden-Format (00:00 bis 23:59)
export function isValidEventTime(time: string): boolean {
  const match = /^(\d{2}):(\d{2})$/.exec(time);

  if (!match) {
    return false;
  }

  const hour = Number(match[1]);
  const minute = Number(match[2]);

  return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
}
