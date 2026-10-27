export const empty = () => ({version:1,students:[],lessons:[],payments:[]});
export const today = () => {const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
export const charge = (rate,minutes,hourly) => Math.round(rate*(hourly?minutes/60:1));
export function balance(db,id,until='9999-12-31') {
 const s=db.students.find(s=>s.id===id); if(!s) return 0;
 return (s.openingDate<=until?s.opening:0)+db.lessons.filter(l=>l.student===id&&l.status==='done'&&l.date<=until).reduce((a,l)=>a+l.amount,0)-db.payments.filter(p=>p.student===id&&p.date<=until).reduce((a,p)=>a+p.amount,0);
}
export function stats(db,from,to) {
 const lessons=db.lessons.filter(l=>l.status==='done'&&l.date>=from&&l.date<=to);
 return {earned:lessons.reduce((a,l)=>a+l.amount,0),received:db.payments.filter(p=>p.date>=from&&p.date<=to).reduce((a,p)=>a+p.amount,0),count:lessons.length,minutes:lessons.reduce((a,l)=>a+l.minutes,0)};
}
const date = v => typeof v==='string' && /^\d{4}-\d{2}-\d{2}$/.test(v) && !Number.isNaN(Date.parse(v)) && new Date(v).toISOString().slice(0,10)===v;
const money = n => Number.isSafeInteger(n)&&Math.abs(n)<=1e12;
export function validate(db) {
 if(!db||db.version!==1||!['students','lessons','payments'].every(k=>Array.isArray(db[k]))) throw Error('Неверный формат резервной копии');
 const ids=new Set();
 for(const s of db.students) {if(typeof s.id!=='string'||ids.has(s.id)||typeof s.name!=='string'||!s.name.trim()||s.name.length>100||!money(s.rate)||s.rate<0||!money(s.opening)||!date(s.openingDate)||!Number.isInteger(s.minutes)||s.minutes<5||s.minutes>1440||typeof s.hourly!=='boolean') throw Error('Некорректные данные ученика');ids.add(s.id);}
 for(const key of ['lessons','payments']) {const seen=new Set();for(const x of db[key]) {if(typeof x.id!=='string'||seen.has(x.id)||!ids.has(x.student)||!date(x.date)||!money(x.amount)||x.amount<0) throw Error('Некорректная операция');seen.add(x.id);if(key==='lessons'&&(!['done','planned','cancelled'].includes(x.status)||!Number.isInteger(x.minutes)||x.minutes<5||x.minutes>1440||typeof x.time!=='string'||!/^([01]\d|2[0-3]):[0-5]\d$/.test(x.time))) throw Error('Некорректное занятие');}}
 return db;
}

// Change the existing lesson instead of creating another financial entry.
export function completeLesson(db,id,date=today()) {
 const lesson=db.lessons.find(l=>l.id===id);
 if(!lesson||lesson.status!=='planned')return false;
 if(lesson.date>date)throw Error('Будущее занятие пока нельзя отметить проведённым');
 lesson.status='done';
 return true;
}
