package main

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRandPassword(t *testing.T) {
	p1 := randPassword(10)
	t.Log(p1)
	assert.Equal(t, len(p1), 10)
	p2 := randPassword(10)
	assert.NotEqual(t, p1, p2)
}
