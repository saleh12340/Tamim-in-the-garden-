import React, { useState } from 'react';
import { StickyNote, Plus, Trash2, Pin, CheckCircle } from 'lucide-react';
import { formatDate } from '../storage';

export default function NotesScreen({ data, onAddNote, onDeleteNote, onTogglePin, showToast }) {
  const { notes = [] } = data;

  const [showAddModal, setShowAddModal] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  const handleSaveNote = () => {
    if (!title.trim() && !content.trim()) {
      if (showToast) showToast('يرجى كتابة عنوان أو تفاصيل للملاحظة', 'danger');
      return;
    }

    onAddNote({
      id: 'note_' + Date.now(),
      title: title.trim() || 'ملاحظة عامة',
      content: content.trim(),
      date: new Date().toISOString(),
      isPinned: false
    });

    setTitle('');
    setContent('');
    setShowAddModal(false);
    if (showToast) showToast('تمت إضافة الملاحظة بنجاح 📌', 'success');
  };

  const sortedNotes = [...notes].sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1;
    if (!a.isPinned && b.isPinned) return 1;
    return new Date(b.date) - new Date(a.date);
  });

  return (
    <div className="notes-screen">
      <div className="section-title">
        <h2>
          <StickyNote size={22} color="#15803d" />
          <span>الملاحظات وقوائم النواقص والطلبيات</span>
        </h2>
        <button className="btn btn-primary btn-sm" onClick={() => setShowAddModal(true)}>
          <Plus size={16} />
          <span>إضافة ملاحظة جديدة</span>
        </button>
      </div>

      {sortedNotes.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
          لا توجد ملاحظات مسجلة. اضغط "إضافة ملاحظة جديدة" لتسجيل نواقص البقالة أو مهام اليوم.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 14 }}>
          {sortedNotes.map((note) => (
            <div 
              key={note.id} 
              className="card" 
              style={{
                position: 'relative',
                background: note.isPinned ? '#fffbeb' : '#ffffff',
                borderColor: note.isPinned ? '#fde68a' : '#e2e8f0',
                boxShadow: note.isPinned ? '0 4px 14px rgba(217, 119, 6, 0.12)' : undefined
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                <strong style={{ fontSize: '1rem', color: '#0f172a' }}>{note.title}</strong>
                <div style={{ display: 'flex', gap: 4 }}>
                  <button 
                    className="btn btn-sm btn-icon" 
                    style={{ background: 'transparent', color: note.isPinned ? '#d97706' : '#94a3b8' }}
                    onClick={() => {
                      onTogglePin(note.id);
                      if (showToast) showToast(note.isPinned ? 'تم إلغاء التثبيت' : 'تم تثبيت الملاحظة في الأعلى', 'success');
                    }}
                    title={note.isPinned ? 'إلغاء التثبيت' : 'تثبيت في الأعلى'}
                  >
                    <Pin size={16} />
                  </button>
                  <button 
                    className="btn btn-sm btn-icon btn-danger" 
                    style={{ background: 'transparent', color: '#ef4444' }}
                    onClick={() => {
                      if (window.confirm('هل تريد حذف هذه الملاحظة؟')) {
                        onDeleteNote(note.id);
                        if (showToast) showToast('تم حذف الملاحظة', 'success');
                      }
                    }}
                    title="حذف الملاحظة"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>

              <div style={{ whiteSpace: 'pre-wrap', color: '#334155', fontSize: '0.9rem', marginBottom: 12, lineHeight: 1.6 }}>
                {note.content}
              </div>

              <div style={{ fontSize: '0.75rem', color: '#64748b', borderTop: '1px dashed #cbd5e1', paddingTop: 6 }}>
                {formatDate(note.date)}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Note Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>إضافة ملاحظة أو طلبية للمحل</h3>
              <button className="btn btn-sm btn-secondary" onClick={() => setShowAddModal(false)}>✕</button>
            </div>

            <div className="form-group">
              <label>عنوان الملاحظة</label>
              <input
                className="form-control"
                placeholder="مثال: طلبيات شركة الألبان ليوم السبت، نواقص المخزن..."
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label>نص الملاحظة / تفاصيل الطلبية</label>
              <textarea
                className="form-control"
                rows={5}
                placeholder="اكتب التفاصيل هنا..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleSaveNote}>
                حفظ الملاحظة
              </button>
              <button className="btn btn-secondary" onClick={() => setShowAddModal(false)}>
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
