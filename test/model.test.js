import {test} from 'node:test';
import assert from 'node:assert/strict';
import {empty,balance,charge,stats,validate} from '../public/model.js';
const sample=()=>({...empty(),students:[{id:'d',name:'Даша',rate:150000,minutes:90,hourly:false,opening:-1200000,openingDate:'2026-10-15'}]});
test('аванс 12000, семь занятий по 1500, платёж 5000: осталось аванса 6500',()=>{const db=sample();db.lessons=Array.from({length:7},(_,i)=>({id:String(i),student:'d',date:'2026-10-16',amount:150000,minutes:90,status:'done',time:'15:00'}));db.payments=[{id:'p',student:'d',date:'2026-10-20',amount:500000}];assert.equal(balance(validate(db),'d','2026-10-21'),-650000);assert.equal(stats(db,'2026-10-15','2026-10-21').earned,1050000);assert.equal(stats(db,'2026-10-15','2026-10-21').received,500000);});
test('почасовой и фиксированный тарифы, округление до копейки',()=>{assert.equal(charge(110000,90,true),165000);assert.equal(charge(150000,90,false),150000);assert.equal(charge(10000,65,true),10833);});
test('запланированные и отменённые занятия не начисляют долг',()=>{const db=sample();db.lessons=['planned','cancelled'].map(status=>({student:'d',date:'2026-10-16',amount:150000,minutes:90,status}));assert.equal(balance(db,'d'),-1200000);assert.equal(stats(db,'2026-10-01','2026-10-31').count,0);});
test('остаток на дату не включает будущие оплаты и начисления',()=>{const db=sample();db.payments=[{student:'d',date:'2026-10-22',amount:100000}];assert.equal(balance(db,'d','2026-10-14'),0);assert.equal(balance(db,'d','2026-10-21'),-1200000);});
test('восстановление отклоняет неверные ссылки, даты и дробные копейки',()=>{for(const change of [d=>d.students[0].rate=1.5,d=>d.students[0].openingDate='2026-02-31',d=>d.payments.push({id:'p',student:'unknown',date:'2026-10-21',amount:100})]){const db=sample();change(db);assert.throws(()=>validate(db));}assert.deepEqual(validate(sample()),sample());});

test('одно нажатие начисляет сохранённую стоимость, повторное не дублирует долг',async()=>{
 const {completeLesson}=await import('../public/model.js');
 const db=sample();db.lessons=[{id:'planned',student:'d',date:'2026-10-20',amount:150000,minutes:90,status:'planned',time:'15:00'}];
 const before=balance(db,'d');
 assert.equal(completeLesson(db,'planned','2026-10-21'),true);
 assert.equal(balance(db,'d'),before+150000);
 assert.equal(stats(db,'2026-10-01','2026-10-31').count,1);
 assert.equal(completeLesson(db,'planned','2026-10-21'),false);
 assert.equal(balance(db,'d'),before+150000);
 assert.equal(db.lessons.length,1);
});
test('быстрая отметка не меняет отменённые или будущие занятия',async()=>{
 const {completeLesson}=await import('../public/model.js');const db=sample();
 db.lessons=[{id:'future',student:'d',date:'2026-10-22',amount:150000,status:'planned'},{id:'cancelled',student:'d',date:'2026-10-20',amount:150000,status:'cancelled'}];
 assert.throws(()=>completeLesson(db,'future','2026-10-21'));
 assert.equal(completeLesson(db,'cancelled','2026-10-21'),false);
 assert.equal(completeLesson(db,'missing','2026-10-21'),false);
 assert.equal(balance(db,'d'),-1200000);
});
