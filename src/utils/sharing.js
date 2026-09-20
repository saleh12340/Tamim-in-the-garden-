/**
 * Helper utilities for sharing invoices, account statements, and WhatsApp messages
 */

import { formatNumber, formatDate } from '../storage';

/**
 * Format phone number to clean international standard for WhatsApp API
 * Handles Yemeni numbers (77xxxxxxx, 73xxxxxxx, 71xxxxxxx, 70xxxxxxx, 077...)
 * Saudi numbers (05xxxxxxxx, 5xxxxxxxx)
 * and generic formats
 */
export function formatWhatsAppPhone(phone) {
  if (!phone) return '';
  let clean = phone.replace(/[^0-9]/g, '');
  if (!clean) return '';

  // Remove leading 00 or 0
  if (clean.startsWith('00')) {
    clean = clean.substring(2);
  }

  // Yemeni local numbers: 77..., 73..., 71..., 70..., 78... (9 digits without leading 0)
  if (/^7[0-9]{8}$/.test(clean)) {
    return '967' + clean;
  }
  if (/^07[0-9]{8}$/.test(clean)) {
    return '967' + clean.substring(1);
  }

  // Saudi numbers: 05... or 5... (9 digits)
  if (/^05[0-9]{8}$/.test(clean)) {
    return '966' + clean.substring(1);
  }
  if (/^5[0-9]{8}$/.test(clean)) {
    return '966' + clean;
  }

  return clean;
}

/**
 * Generate formatted text for a sales invoice
 */
export function generateInvoiceShareText(invoice, storeInfo) {
  const sName = storeInfo?.name || 'بقالة العنزي للمواد الغذائية';
  const sPhone = storeInfo?.phone || '776425052';
  const currency = storeInfo?.currency || 'ريال';

  const lines = [
    `🧾 *${sName}*`,
    `📞 هاتف: ${sPhone}`,
    `━━━━━━━━━━━━━━━━━━━━`,
    `📋 *فاتورة مبيعات رقم:* #${invoice.number}`,
    `📅 التاريخ: ${formatDate(invoice.date)}`,
    `👤 العميل: ${invoice.customerName || 'عميل نقدي'}`,
    `━━━━━━━━━━━━━━━━━━━━`,
    `*تفاصيل الأصناف:*`,
    ...(invoice.items || []).map((item, idx) => 
      `${idx + 1}. *${item.name}*\n   الكمية: ${item.qty} × ${formatNumber(item.price)} = ${formatNumber(item.total)} ${currency}`
    ),
    `━━━━━━━━━━━━━━━━━━━━`,
    `💰 *الإجمالي الكلي:* ${formatNumber(invoice.total)} ${currency}`,
    `💵 المدفوع: ${formatNumber(invoice.paid)} ${currency}`,
    `💳 *المتبقي:* ${formatNumber(invoice.remaining)} ${currency}`,
    invoice.notes ? `📝 ملاحظات: ${invoice.notes}` : '',
    `━━━━━━━━━━━━━━━━━━━━`,
    `🙏 شكراً لتعاملكم معنا ونسعد دائماً بخدمتكم!`
  ].filter(Boolean);

  return lines.join('\n');
}

/**
 * Generate formatted text for Customer Account Statement (كشف الحساب)
 */
export function generateCustomerStatementText(customer, transactions = [], storeInfo) {
  const sName = storeInfo?.name || 'بقالة العنزي للمواد الغذائية';
  const sPhone = storeInfo?.phone || '776425052';
  const currency = storeInfo?.currency || 'ريال';

  const customerTx = transactions
    .filter(tx => tx.customerId === customer.id)
    .sort((a, b) => new Date(b.date) - new Date(a.date));

  const totalDebit = customerTx.filter(t => t.type === 'debit').reduce((sum, t) => sum + (t.amount || 0), 0);
  const totalCredit = customerTx.filter(t => t.type === 'credit').reduce((sum, t) => sum + (t.amount || 0), 0);

  const lines = [
    `🏢 *${sName}*`,
    `📞 للتواصل: ${sPhone}`,
    `━━━━━━━━━━━━━━━━━━━━`,
    `📑 *كشف حساب العميل:* ${customer.name}`,
    `📱 الهاتف: ${customer.phone || 'غير مسجل'}`,
    `📅 تاريخ الكشف: ${new Date().toLocaleDateString('ar-EG')}`,
    `━━━━━━━━━━━━━━━━━━━━`,
    `🔴 *الرصيد المستحق حالياً:* ${formatNumber(customer.balance)} ${currency} ${customer.balance > 0 ? '(مطلوب سداده)' : customer.balance < 0 ? '(دائن / له)' : '(خالص)'}`,
    `━━━━━━━━━━━━━━━━━━━━`,
    `📊 *ملخص الحركات:*`,
    `• إجمالي عليه (مشتريات/ديون): ${formatNumber(totalDebit)} ${currency}`,
    `• إجمالي سدد (دفعات نقدية): ${formatNumber(totalCredit)} ${currency}`,
    `━━━━━━━━━━━━━━━━━━━━`,
    `📋 *آخر الحركات والعمليات:*`,
    ...customerTx.slice(0, 10).map((tx, idx) => {
      const typeLabel = tx.type === 'debit' ? '🔴 عليه (+)' : '🟢 سدد (-)';
      return `${idx + 1}. ${typeLabel} ${formatNumber(tx.amount)} ${currency}\n   البيان: ${tx.details} | ${new Date(tx.date).toLocaleDateString('ar-EG')}`;
    }),
    `━━━━━━━━━━━━━━━━━━━━`,
    `🤝 شاكرين حسن تعاملكم والتزامكم معنا.`
  ];

  return lines.join('\n');
}

/**
 * Open WhatsApp with pre-filled text and targeted phone number
 */
export function shareViaWhatsApp(phone, text) {
  const formattedPhone = formatWhatsAppPhone(phone);
  const encodedText = encodeURIComponent(text);

  let url;
  if (formattedPhone) {
    url = `https://api.whatsapp.com/send?phone=${formattedPhone}&text=${encodedText}`;
  } else {
    url = `https://api.whatsapp.com/send?text=${encodedText}`;
  }

  window.open(url, '_blank', 'noopener,noreferrer');
}

/**
 * Trigger Native Web Share or fallback to WhatsApp / Clipboard
 */
export async function shareContentNative({ title, text, phone }) {
  if (navigator.share) {
    try {
      await navigator.share({
        title: title || 'مشاركة من بقالة العنزي',
        text: text
      });
      return { success: true, method: 'native' };
    } catch (err) {
      if (err.name !== 'AbortError') {
        // If native share fails or cancelled, fallback to WhatsApp
        shareViaWhatsApp(phone, text);
        return { success: true, method: 'whatsapp' };
      }
      return { success: false, method: 'aborted' };
    }
  } else {
    // Fallback to WhatsApp
    shareViaWhatsApp(phone, text);
    return { success: true, method: 'whatsapp' };
  }
}

/**
 * Copy text to clipboard with modern promise
 */
export async function copyToClipboard(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch (err) {
      console.warn('Clipboard write failed, using textarea fallback', err);
    }
  }

  // Fallback
  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    const success = document.execCommand('copy');
    document.body.removeChild(textarea);
    return success;
  } catch (err) {
    console.error('Fallback copy failed', err);
    return false;
  }
}
