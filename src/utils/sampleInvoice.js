/**
 * Sample scanned invoice graphic generator for initial gallery preview
 */
export function getSampleInvoiceDataUrl() {
  const canvas = document.createElement('canvas');
  canvas.width = 600;
  canvas.height = 820;
  const ctx = canvas.getContext('2d');

  // Paper background with subtle texture
  ctx.fillStyle = '#f8fafc';
  ctx.fillRect(0, 0, 600, 820);

  // Border & Header
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(20, 20, 560, 780);

  ctx.strokeStyle = '#cbd5e1';
  ctx.lineWidth = 2;
  ctx.strokeRect(20, 20, 560, 780);

  // Header Banner
  ctx.fillStyle = '#15803d';
  ctx.fillRect(20, 20, 560, 90);

  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 26px Cairo, sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('شركة السعيد للتجارة العامة والتوريد', 300, 60);

  ctx.font = '16px Cairo, sans-serif';
  ctx.fillText('فاتورة توريد بضاعة - قسم المواد الغذائية', 300, 90);

  // Meta Info
  ctx.fillStyle = '#334155';
  ctx.font = '15px Cairo, sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText('رقم الفاتورة: #PUR-8902', 550, 140);
  ctx.fillText('التاريخ: 2026/09/20', 550, 168);
  ctx.fillText('المورد: مستودع الأمانة المركزي', 550, 196);
  ctx.fillText('العميل: بقالة العنزي للمواد الغذائية', 550, 224);

  ctx.textAlign = 'left';
  ctx.fillText('طريقة الدفع: نقداً', 50, 140);
  ctx.fillText('حالة التوريد: تم الاستلام', 50, 168);
  ctx.fillText('رقم السجل: 1089201', 50, 196);

  // Table header
  ctx.fillStyle = '#e2e8f0';
  ctx.fillRect(40, 250, 520, 36);

  ctx.fillStyle = '#0f172a';
  ctx.font = 'bold 15px Cairo, sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText('البيان / الصنف', 540, 274);
  ctx.fillText('الكمية', 340, 274);
  ctx.fillText('السعر', 230, 274);
  ctx.fillText('الإجمالي', 120, 274);

  // Table rows
  const items = [
    { name: 'أرز الشعلان بنجابي 10 كجم', qty: '10 كيس', price: '42,000', total: '420,000' },
    { name: 'سمن شوكة وملعقة أصلي 1.8 كجم', qty: '12 حبة', price: '18,500', total: '222,000' },
    { name: 'زيت عافية ذرة نقي 1.5 لتر', qty: '24 حبة', price: '7,500', total: '180,000' },
    { name: 'سكر الأسرة ناعم 10 كجم', qty: '8 كيس', price: '28,000', total: '224,000' },
    { name: 'حليب الممتاز كرتون 48 حبة', qty: '2 كرتون', price: '98,000', total: '196,000' }
  ];

  let y = 320;
  ctx.font = '14px Cairo, sans-serif';
  items.forEach((item, i) => {
    if (i % 2 === 1) {
      ctx.fillStyle = '#f8fafc';
      ctx.fillRect(40, y - 22, 520, 32);
    }
    ctx.fillStyle = '#1e293b';
    ctx.textAlign = 'right';
    ctx.fillText(item.name, 540, y);
    ctx.fillText(item.qty, 340, y);
    ctx.fillText(item.price, 230, y);
    ctx.fillText(item.total, 120, y);

    ctx.strokeStyle = '#f1f5f9';
    ctx.beginPath();
    ctx.moveTo(40, y + 10);
    ctx.lineTo(560, y + 10);
    ctx.stroke();

    y += 40;
  });

  // Summary box
  ctx.fillStyle = '#f0fdf4';
  ctx.fillRect(40, 560, 520, 110);
  ctx.strokeStyle = '#86efac';
  ctx.strokeRect(40, 560, 520, 110);

  ctx.fillStyle = '#166534';
  ctx.font = 'bold 16px Cairo, sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText('إجمالي فاتورة التوريد:', 540, 600);
  ctx.font = 'bold 22px Cairo, sans-serif';
  ctx.fillText('1,242,000 ريال', 200, 600);

  ctx.font = '14px Cairo, sans-serif';
  ctx.fillText('المدفوع: 1,242,000 ريال (مسدد بالكامل)', 540, 640);

  // Stamp / Watermark
  ctx.save();
  ctx.translate(140, 730);
  ctx.rotate(-0.15);
  ctx.strokeStyle = '#15803d';
  ctx.lineWidth = 3;
  ctx.strokeRect(-80, -25, 160, 50);
  ctx.fillStyle = '#15803d';
  ctx.font = 'bold 16px Cairo, sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('مستلم ومطابق للمخزن', 0, 5);
  ctx.restore();

  // Footer notes
  ctx.fillStyle = '#64748b';
  ctx.font = '12px Cairo, sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('تمت المسح الضوئي بواسطة الماسح الذكي - بقالة العنزي', 300, 770);
  ctx.fillText('مجلد التخزين: invoices_images', 300, 790);

  return canvas.toDataURL('image/jpeg', 0.92);
}
