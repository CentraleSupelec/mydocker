package main

import (
	"encoding/json"
	"fmt"
	"sync"
)

const autoscalingStateName string = "autoscaling.tf"

type TerraformState struct {
	Resources []struct {
		Type      string `json:"type"`
		Name      string `json:"name"`
		Instances []struct {
			Attributes          map[string]Attribute `json:"attributes"`
			SensitiveAttributes []interface{}        `json:"sensitive_attributes"`
			Dependencies        []string             `json:"dependencies"`
		} `json:"instances"`
	} `json:"resources"`
}

type Attribute struct {
	AttributeString string
	AttributeMap    map[string]Attribute
	AttributeList   []Attribute
	AttributeBool   bool
	AttributeFloat  float64
}

func (a *Attribute) UnmarshalJSON(input []byte) error {
	var err error

	var float float64
	err = json.Unmarshal(input, &float)
	if err == nil {
		a.AttributeFloat = float
		return nil
	}

	var text string
	err = json.Unmarshal(input, &text)
	if err == nil {
		a.AttributeString = text
		return nil
	}

	var hashmap map[string]Attribute
	err = json.Unmarshal(input, &hashmap)
	if err == nil {
		a.AttributeMap = hashmap
		return nil
	}

	var list []Attribute
	err = json.Unmarshal(input, &list)
	if err == nil {
		a.AttributeList = list
		return nil
	}

	var boolean bool
	err = json.Unmarshal(input, &boolean)
	if err == nil {
		a.AttributeBool = boolean
		return nil
	}

	return err
}

type AutoscalingUtilsInterface interface {
	buildConfigFromExistingInfra() (*TerraformConfig, error)
}

type AutoscalingUtils struct{}

func (a *AutoscalingUtils) buildConfigFromExistingInfra() (*TerraformConfig, error) {
	terraformConfig := NewTerraformConfig(autoscalingStateName)
	state := &TerraformState{}
	err := getJson(fmt.Sprintf("%s/%s", c.AutoscalingStateBaseUrl, autoscalingStateName), state, true)
	if err != nil {
		return nil, err
	}
	for _, resource := range state.Resources {
		if resource.Type == "openstack_compute_instance_v2" {
			for _, instance := range resource.Instances {
				terraformConfig.NamedWorkers[instance.Attributes["name"].AttributeString] = TerraformNamedWorker{
					InstanceImageId: instance.Attributes["image_id"].AttributeString,
					InstanceType:    instance.Attributes["flavor_name"].AttributeString,
					Name:            instance.Attributes["name"].AttributeString,
					Region:          instance.Attributes["region"].AttributeString,
					Owner:           instance.Attributes["metadata"].AttributeMap["owner"].AttributeString,
				}
			}
		}
	}
	return terraformConfig, nil
}

type scaleUpOwner struct {
	InstanceType      string
	MinIdleNodesCount int64
	MaxNodesCount     int64
	ManualNodesCount  int64
	Regions           []ScalingRegion
}

type ScaleUpConfig struct {
	Lock          sync.Mutex
	ScaleUpOwners map[string]scaleUpOwner
}

type ScalingRegion struct {
	ImageId string
	Region  string
}
