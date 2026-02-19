-- KEYS[1]: 버퍼 키 (chat:buffer:{partyId})
-- ARGV[1]: 새로 추가할 메시지 (JSON String)

-- 1. 메시지를 리스트 끝에 추가 (RPUSH)
redis.call('RPUSH', KEYS[1], ARGV[1])

-- 2. 현재 버퍼 길이 확인 (LLEN)
local len = redis.call('LLEN', KEYS[1])

-- 3. 길이가 5 이상이면 모두 꺼내고 삭제
if len >= 5 then
    local items = redis.call('LRANGE', KEYS[1], 0, -1)
    redis.call('DEL', KEYS[1])
    return items
else
    return nil
end
