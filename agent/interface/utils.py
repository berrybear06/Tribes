def one_hot(pos, length):
	assert -1 <= pos < length
	result = [0] * length
	if pos != -1:
		result[pos] = 1
	return result
