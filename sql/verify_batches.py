# 模拟脚本中所有 batch 类型的实际格式
print('='*60)
print('Complete batch simulation (ACTUAL script format)')
print('='*60)

inserted_ids = list(range(1000, 1045))
advisor_id = 500
inserted_app_ids = list(range(2000, 2200))
first_emp_id = 3000

# ALL batch types use: (inner_tuple, student_local_idx)
# Prepend: (inserted_ids[li],) + e[1] -> WRONG
# Prepend: (inserted_ids[li],) + e[0] -> CORRECT (e[0] is inner)

# 1. student_resume: (inner16, student_local_idx)
print('\n1. student_resume')
batch_resumes = [(('name', '1', 'summary', 'edu', 'proj', '', 'cert', '', 'eval', 6000, 10000, 'city', 'pos', 'ind', '',), 0)]
resumes_with_uid = [(inserted_ids[li],) + r[0] for li, r in batch_resumes]
print('  resumes_with_uid len: %d (expected 16)' % len(resumes_with_uid[0]))
assert len(resumes_with_uid[0]) == 16

# 2. job_application: (inner9, student_local_idx)
print('\n2. job_application')
batch_apps = [((50, 20, None, 'accepted', '1', 'letter', '', 'interviewed', 'offered'), 0)]
apps_with_uid = [(inserted_ids[li],) + a[0] for li, a in batch_apps]
print('  apps_with_uid len: %d (expected 10)' % len(apps_with_uid[0]))
assert len(apps_with_uid[0]) == 10

# 3. interview_invitation: (inner11, student_local_idx)
print('\n3. interview_invitation')
batch_interviews = [((None, None, 20, 50, 'time', 'addr', 'type', 'person', 'phone', '', 'completed'), 0)]
app_seq_map = {li: seq for seq, (_, li) in enumerate(batch_apps)}
intv_final = []
for inner, li in batch_interviews:
    app_seq = app_seq_map.get(li, -1)
    app_id = inserted_app_ids[app_seq] if app_seq >= 0 else None
    intv_final.append((app_id, inserted_ids[li]) + inner[2:])
print('  intvs_with_uid len: %d (expected 11)' % len(intv_final[0]))
assert len(intv_final[0]) == 11

# 4. offer_letter: (inner12, student_local_idx)
print('\n4. offer_letter')
batch_offers = [((None, None, 20, 50, 'pos', 'sal', 'city', 'date', 'period', 'prosal', 'deadline', 'accepted'), 0)]
offer_final = []
for inner, li in batch_offers:
    app_seq = app_seq_map.get(li, -1)
    app_id = inserted_app_ids[app_seq] if app_seq >= 0 else None
    offer_final.append((app_id, inserted_ids[li]) + inner[2:])
print('  offers_with_uid len: %d (expected 12)' % len(offer_final[0]))
assert len(offer_final[0]) == 12

# 5. employment_record: (inner21, student_local_idx)
print('\n5. employment_record')
batch_employments = [(( 'type', 'company', 'code', 'scale', 'ind', 'pos', '', 'city', 'prov', 'sal', '1', 'TP001', 'start', 'end', 'prosal', 'pending', '', None, None, None, ''), 0)]
emps_with_uid = [(inserted_ids[li],) + e[0] for li, e in batch_employments]
print('  emps_with_uid len: %d (expected 21)' % len(emps_with_uid[0]))
assert len(emps_with_uid[0]) == 21

# 6. conversation_record: (inner8, student_local_idx)
print('\n6. conversation_record')
batch_conversations = [(('type', 'time', 'place', 'topic', 'content', 'result', 'plan', ''), 0)]
conv_final = [(inserted_ids[li], advisor_id) + c[0] for li, c in batch_conversations]
print('  convs_with_uid len: %d (expected 10)' % len(conv_final[0]))
assert len(conv_final[0]) == 10

# 7. tripartite_agreement: (inner7, student_local_idx)
print('\n7. tripartite_agreement')
batch_agreements = [((20, 'TP001', 'stime', 'ctime', 'shtime', '', 'completed'), 0)]
inserted_emp_ids = list(range(first_emp_id, first_emp_id + len(emps_with_uid)))
emp_seq_map = {li: seq for seq, (_, li) in enumerate(batch_employments)}
agr_final = []
for inner, li in batch_agreements:
    emp_seq = emp_seq_map.get(li, -1)
    emp_id = inserted_emp_ids[emp_seq] if emp_seq >= 0 else None
    agr_final.append((inserted_ids[li], emp_id) + inner)
print('  agreements_with_uid len: %d (expected 9)' % len(agr_final[0]))
assert len(agr_final[0]) == 9

print()
print('='*60)
print('ALL 7 TABLES PASSED!')
print('='*60)
