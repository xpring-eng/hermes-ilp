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
local sender_account_id = ARGV[1]
local original_amount = numberOrZero(ARGV[2])
local amount_left_to_send = numberOrZero(ARGV[3])
if (isempty(original_amount)) then
    error("original_amount was nil!") -- should never happen!
end

local destination = ARGV[4]
local status = 'PENDING'
redis.call('HSET', payment_id,
    'sender_account_id', sender_account_id,
    'original_amount', original_amount,
    'amount_left_to_send', amount_left_to_send,
    'destination', destination,
    'status', status)
