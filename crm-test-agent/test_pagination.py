import requests

r = requests.post('http://localhost:8081/api/v1/auth/login', json={'email':'testhealth@crm.com','password':'Test1878','tenantId':'healthcare'}, timeout=10)
t = r.json()['data']['accessToken']
h = {'Authorization': f'Bearer {t}'}

for page in range(3):
    r1 = requests.get(f'http://localhost:3000/api/v1/leads?page={page}&size=10&sortBy=createdAt&sortDir=desc', headers=h, timeout=15)
    d = r1.json()['data']
    content = d['content']
    total = d['totalElements']
    pages = d['totalPages']
    first = content[0]['firstName'] + ' ' + content[0]['lastName'] if content else 'N/A'
    last = content[-1]['firstName'] + ' ' + content[-1]['lastName'] if content else 'N/A'
    print(f"Page {page}: {len(content)} leads, total={total}, pages={pages}, first={first}, last={last}")
