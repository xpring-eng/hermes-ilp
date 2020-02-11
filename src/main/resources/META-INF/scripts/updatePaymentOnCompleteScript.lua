local function isempty(s)
    return s == nil or s == '' or s == false
end

local function numberOrZero(num)
    if (isempty(num)) then
        return 0
    else
        return tonumber(num)
    end
end

local payment_id = KEYS[1]
local amount_sent = numberOrZero(ARGV[1])
local amount_delivered = numberOrZero(ARGV[2])
local amount_left_to_send = numberOrZero(ARGV[3])
local status = ARGV[4]

redis.call('HSET', payment_id,
    'amount_sent', amount_sent,
    'amount_delivered', amount_delivered,
    'amount_left_to_send', amount_left_to_send,
    'status', status)
